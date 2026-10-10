package com.gyote.silvercare.health_record.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyote.silvercare.global.auth.application.JwtService;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.*;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockHttpSession;
import jakarta.servlet.http.Cookie;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HealthRecordApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;
    @Autowired PatientRepository patients;
    @Autowired CareRelationCommandService cares;
    User personal;
    String token;
    @BeforeEach void setup() {
        personal=accounts.chooseRole(accounts.loginOrRegister("http-"+java.util.UUID.randomUUID(),"본인").getKakaoId(),UserRole.PATIENT);
        token=jwt.create(personal);
    }
    String record(String body) throws Exception {
        var result=mvc.perform(post("/api/health-records").header("Authorization","Bearer "+token)
                .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("body",body))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.authorUserId").value(personal.getId().toString()))
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("recordId").asText();
    }
    @Test void crudAndSoftDeleteThroughHttp() throws Exception {
        String id=record("내 기록");
        mvc.perform(put("/api/health-records/"+id).header("Authorization","Bearer "+token)
                .contentType("application/json").content("{\"body\":\"대상 변경\",\"patientId\":\""+java.util.UUID.randomUUID()+"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/health-records/"+id).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.body").value("내 기록"));
        mvc.perform(get("/api/health-records").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].recordId").value(id));
        mvc.perform(put("/api/health-records/"+id).header("Authorization","Bearer "+token)
                .contentType("application/json").content("{\"body\":\"수정 기록\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.body").value("수정 기록"));
        mvc.perform(delete("/api/health-records/"+id).header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/health-records/"+id).header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
        mvc.perform(get("/api/health-records").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
    }
    @Test void switchingChangesSessionAndJwtAndRestoresExistingPersonalRecords() throws Exception {
        String id=record("전환 전 기록");
        var switched=mvc.perform(post("/api/accounts/switch").header("Authorization","Bearer "+token)
                .contentType("application/json").content("{\"role\":\"CAREGIVER\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CAREGIVER")).andReturn();
        MockHttpSession session=(MockHttpSession)switched.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CAREGIVER"));
        String cookieHeader=switched.getResponse().getHeader("Set-Cookie");
        assertThat(cookieHeader).startsWith("SILVERCARE_TOKEN=");
        Cookie cookie=new Cookie("SILVERCARE_TOKEN",cookieHeader.split(";",2)[0].substring("SILVERCARE_TOKEN=".length()));
        mvc.perform(get("/api/me").cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CAREGIVER"));
        mvc.perform(get("/api/health-records/"+id).cookie(cookie)).andExpect(status().isForbidden());
        var back=mvc.perform(post("/api/accounts/switch").session(session).contentType("application/json")
                .content("{\"role\":\"PATIENT\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(personal.getId().toString())).andReturn();
        mvc.perform(get("/api/health-records/"+id).session((MockHttpSession)back.getRequest().getSession(false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.body").value("전환 전 기록"));
    }
    @Test void unauthenticatedAndInvalidInputsAreRejected() throws Exception {
        mvc.perform(get("/api/health-records")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/health-records").header("Authorization","Bearer "+token)
                .contentType("application/json").content("{\"body\":\"  \"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/health-records").header("Authorization","Bearer "+token)
                .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("body","가".repeat(2001)))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/health-records?size=101").header("Authorization","Bearer "+token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/health-records?cursor=wrong").header("Authorization","Bearer "+token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/health-records/not-a-uuid").header("Authorization","Bearer "+token))
                .andExpect(status().isBadRequest());
    }
    @Test void personalEditingProxyRecordKeepsOriginalAuthorAfterUnlink() throws Exception {
        User caregiver=accounts.chooseRole(accounts.loginOrRegister("proxy-"+java.util.UUID.randomUUID(),"원래 보호자").getKakaoId(),UserRole.CAREGIVER);
        var relation=cares.request(caregiver,accounts.patientInviteCode(personal));
        cares.accept(personal,relation.getId());
        String patientId=patients.findByUserId(personal.getId()).orElseThrow().getId().toString();
        var created=mvc.perform(post("/api/health-records").header("Authorization","Bearer "+jwt.create(caregiver))
                .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("body","보호자가 작성", "patientId",patientId))))
                .andExpect(status().isCreated()).andReturn();
        String id=json.readTree(created.getResponse().getContentAsString()).get("recordId").asText();
        cares.revoke(personal,relation.getId());
        mvc.perform(put("/api/health-records/"+id).header("Authorization","Bearer "+token)
                .contentType("application/json").content("{\"body\":\"본인이 수정\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.body").value("본인이 수정"))
                .andExpect(jsonPath("$.authorUserId").value(caregiver.getId().toString()))
                .andExpect(jsonPath("$.authorName").value("원래 보호자"))
                .andExpect(jsonPath("$.proxyWritten").value(true));
    }
    @Test void linkedCaregiverCanReadButCannotChangeOtherAuthorsRecord() throws Exception {
        String id=record("본인 기록");
        User caregiver=accounts.chooseRole(accounts.loginOrRegister("other-"+java.util.UUID.randomUUID(),"보호자").getKakaoId(),UserRole.CAREGIVER);
        var relation=cares.request(caregiver,accounts.patientInviteCode(personal));
        String caregiverToken=jwt.create(caregiver);
        mvc.perform(get("/api/health-records/"+id).header("Authorization","Bearer "+caregiverToken))
                .andExpect(status().isForbidden());
        cares.accept(personal,relation.getId());
        mvc.perform(get("/api/health-records/"+id).header("Authorization","Bearer "+caregiverToken))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/health-records/"+id).header("Authorization","Bearer "+caregiverToken))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("HEALTH_RECORD_003"));
        cares.revoke(personal,relation.getId());
        mvc.perform(get("/api/health-records/"+id).header("Authorization","Bearer "+caregiverToken))
                .andExpect(status().isForbidden());
    }
}
