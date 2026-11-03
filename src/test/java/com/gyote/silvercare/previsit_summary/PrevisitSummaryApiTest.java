package com.gyote.silvercare.previsit_summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.global.auth.application.JwtService;
import com.gyote.silvercare.health_record.domain.entity.HealthRecord;
import com.gyote.silvercare.health_record.domain.repository.HealthRecordRepository;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.previsit_summary.command.application.PrevisitSummaryCommandService;
import com.gyote.silvercare.previsit_summary.domain.PrevisitGenerationPort;
import com.gyote.silvercare.previsit_summary.domain.repository.PrevisitSummaryRepository;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 인증·영속 요청·권한·근거 신선도·상태 전이를 검증한다. */
@SpringBootTest(properties={"silvercare.previsit.enabled=true","silvercare.previsit.processing-enabled=false"})
@AutoConfigureMockMvc
@Transactional
class PrevisitSummaryApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired PatientRepository patients;
    @Autowired HealthRecordRepository records;
    @Autowired PrevisitSummaryRepository jobs;
    @Autowired PrevisitSummaryCommandService commands;
    @Autowired CareRelationCommandService cares;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @MockBean PrevisitGenerationPort provider;
    User personal,caregiver,other;
    UUID patient;
    String token,careToken,otherToken;

    @BeforeEach
    void setup() {
        String id=UUID.randomUUID().toString();
        personal=user("p"+id,UserRole.PATIENT); caregiver=user("c"+id,UserRole.CAREGIVER);
        other=user("o"+id,UserRole.PATIENT);
        patient=patients.findByUserId(personal.getId()).orElseThrow().getId();
        token=jwt.create(personal); careToken=jwt.create(caregiver); otherToken=jwt.create(other);
        when(provider.available()).thenReturn(true);
        when(provider.generate(any())).thenAnswer(call -> {
            var input=(PrevisitGenerationPort.Input)call.getArgument(0);
            return new PrevisitGenerationPort.Result(input.requestId(),"산책했다고 기록했습니다.",
                    input.records().stream().map(PrevisitGenerationPort.Source::healthRecordId).toList(),
                    "COMPLETE",false,null,false);
        });
    }
    User user(String id,UserRole role) {
        var u=accounts.loginOrRegister("previsit-"+id,"시연");
        return accounts.chooseRole(u.getKakaoId(),role);
    }
    HealthRecord record(String content) {
        return records.saveAndFlush(HealthRecord.create(patient,personal.getId(),null,content,false));
    }
    UUID create(String bearer,String body,String key) throws Exception {
        var result=mvc.perform(post("/api/previsit-summaries").header("Authorization","Bearer "+bearer)
                .header("Idempotency-Key",key).contentType("application/json").content(body))
                .andExpect(status().isAccepted()).andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(result).get("requestId").asText());
    }
    UUID create() throws Exception {return create(token,"{}",UUID.randomUUID().toString());}
    String targetBody(){return "{\"patientId\":\""+patient+"\"}";}
    UUID summaryId(UUID request){return jobs.findById(request).orElseThrow().getSummaryId();}

    @Test
    void recentFourteenDaysExcludeOldFutureDeletedAndOtherPatients() throws Exception {
        var included=record("산책 10분"); var old=record("오래된 기록"); var future=record("미래 기록");
        var deleted=record("삭제 기록"); deleted.delete(); records.flush();
        var otherPatient=patients.findByUserId(other.getId()).orElseThrow().getId();
        records.saveAndFlush(HealthRecord.create(otherPatient,other.getId(),null,"다른 개인 기록",false));
        jdbc.update("update health_records set recorded_at=? where id=?",Instant.now().minusSeconds(15*86400),old.getId());
        jdbc.update("update health_records set recorded_at=? where id=?",Instant.now().plusSeconds(3600),future.getId());
        em.clear();
        UUID request=create();
        mvc.perform(get("/api/ai-operations/"+request).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.jobStatus").value("QUEUED"))
                .andExpect(jsonPath("$.summaryId").isEmpty());
        assertThat(commands.processNext()).isTrue();
        verify(provider).generate(argThat(input -> input.windowDays()==14 && input.records().size()==1
                && input.records().get(0).healthRecordId().equals(included.getId())));
        assertThat(summaryId(request)).isNotEqualTo(request);
        mvc.perform(get("/api/previsit-summaries/"+summaryId(request)).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usedHealthRecordIds[0]").value(included.getId().toString()))
                .andExpect(jsonPath("$.sourceRecords[0].content").value("산책 10분"));
    }

    @Test
    void noRecordsReturnsInsufficientWithoutCallingAi() throws Exception {
        UUID request=create();
        mvc.perform(get("/api/ai-operations/"+request).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.jobStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.resultStatus").value("INSUFFICIENT"));
        verify(provider,never()).generate(any());
        mvc.perform(get("/api/previsit-summaries/"+summaryId(request)).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sourceRecords").isEmpty());
    }

    @Test
    void requestIsIdempotentButDifferentWindowConflicts() throws Exception {
        record("기록"); String key=UUID.randomUUID().toString();
        UUID first=create(token,"{}",key);
        assertThat(create(token,"{}",key)).isEqualTo(first);
        mvc.perform(post("/api/previsit-summaries").header("Authorization","Bearer "+token)
                .header("Idempotency-Key",key).contentType("application/json").content("{\"windowDays\":7}"))
                .andExpect(status().isConflict());
        assertThat(jobs.findAll()).hasSize(1);
    }

    @Test
    void requestedCaregiverAndOtherPatientAreDeniedWhileActiveCanRequestAndRead() throws Exception {
        record("기록"); UUID request=create();
        var relation=cares.request(caregiver,accounts.patientInviteCode(personal));
        for(String bearer:List.of(careToken,otherToken))
            mvc.perform(get("/api/ai-operations/"+request).header("Authorization","Bearer "+bearer))
                    .andExpect(status().isForbidden());
        cares.accept(personal,relation.getId());
        UUID careRequest=create(careToken,targetBody(),UUID.randomUUID().toString());
        mvc.perform(get("/api/ai-operations/"+careRequest).header("Authorization","Bearer "+careToken))
                .andExpect(status().isOk());
        cares.revoke(personal,relation.getId());
        mvc.perform(get("/api/ai-operations/"+careRequest).header("Authorization","Bearer "+careToken))
                .andExpect(status().isForbidden());
        commands.processNext(); commands.processNext();
        assertThat(jobs.findById(careRequest).orElseThrow().getJobStatus()).isEqualTo("FAILED");
    }

    @Test
    void deletedSourceBeforeProcessingDoesNotReachAi() throws Exception {
        var source=record("기록"); UUID request=create();
        source.delete(); records.flush(); commands.processNext();
        verify(provider,never()).generate(any());
        assertThat(jobs.findById(request).orElseThrow().getJobStatus()).isEqualTo("FAILED");
        mvc.perform(get("/api/ai-operations/"+request).header("Authorization","Bearer "+token))
                .andExpect(status().isConflict());
    }

    @Test
    void sourceChangedDuringGenerationCannotBeSaved() throws Exception {
        var source=record("원래 기록"); UUID request=create();
        doAnswer(call -> {
            source.update("고친 기록",null); records.flush();
            var input=(PrevisitGenerationPort.Input)call.getArgument(0);
            return new PrevisitGenerationPort.Result(input.requestId(),"이전 본문",List.of(source.getId()),"COMPLETE",false,null,false);
        }).when(provider).generate(any());
        commands.processNext();
        assertThat(jobs.findById(request).orElseThrow().getSummaryId()).isNull();
        assertThat(jobs.findById(request).orElseThrow().getJobStatus()).isEqualTo("FAILED");
    }

    @Test
    void editingAfterSuccessHidesTheOldSummary() throws Exception {
        var source=record("기록"); UUID request=create(); commands.processNext();
        UUID result=summaryId(request); source.update("수정",null); records.flush();
        mvc.perform(get("/api/previsit-summaries/"+result).header("Authorization","Bearer "+token))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PREVISIT_004"));
    }

    @Test
    void inventedSourceAndProviderErrorBecomeSafeFailures() throws Exception {
        record("기록"); UUID request=create();
        doReturn(new PrevisitGenerationPort.Result(request,"가짜 근거",
                List.of(UUID.randomUUID()),"COMPLETE",false,null,false)).when(provider).generate(any());
        commands.processNext();
        assertThat(jobs.findById(request).orElseThrow().getErrorCode()).isEqualTo("AI_RESPONSE_INVALID");
        UUID next=create(); doThrow(new IllegalStateException("private provider body")).when(provider).generate(any());
        commands.processNext();
        mvc.perform(get("/api/ai-operations/"+next).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.jobStatus").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value("AI_UNAVAILABLE"));
    }

    @Test
    void invalidInputAndMissingGuardianTargetAreRejected() throws Exception {
        for(String body:List.of("{\"windowDays\":0}","{\"windowDays\":31}"))
            mvc.perform(post("/api/previsit-summaries").header("Authorization","Bearer "+token)
                    .header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        mvc.perform(post("/api/previsit-summaries").header("Authorization","Bearer "+careToken)
                .header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/ai-operations/"+UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void unavailableProviderRejectsNewNonemptyRequestsWithoutSaving() throws Exception {
        record("기록"); when(provider.available()).thenReturn(false);
        mvc.perform(post("/api/previsit-summaries").header("Authorization","Bearer "+token)
                .header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content("{}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(jobs.count()).isZero();
    }

    @Test
    void tooManyRecordsAreNotSilentlyDropped() throws Exception {
        for(int i=0;i<101;i++) record("기록 "+i);
        mvc.perform(post("/api/previsit-summaries").header("Authorization","Bearer "+token)
                .header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PREVISIT_003"));
        verify(provider,never()).generate(any()); assertThat(jobs.count()).isZero();
    }

    @Test
    void interruptedJobExpiresWithoutAutomaticProviderRecall() throws Exception {
        record("기록"); UUID request=create(); var row=jobs.findById(request).orElseThrow();
        row.start(); jobs.flush();
        jdbc.update("update previsit_summaries set started_at=? where id=?",Instant.now().minusSeconds(601),request);
        em.clear(); assertThat(commands.processNext()).isFalse();
        assertThat(jobs.findById(request).orElseThrow().getErrorCode()).isEqualTo("AI_TIMEOUT");
        verify(provider,never()).generate(any());
    }

    @Test
    void revokedGuardianCannotReadPreviouslyCompletedSummary() throws Exception {
        record("기록"); UUID request=create(); commands.processNext();
        var relation=cares.request(caregiver,accounts.patientInviteCode(personal));
        cares.accept(personal,relation.getId());
        String url="/api/previsit-summaries/"+summaryId(request);
        mvc.perform(get(url).header("Authorization","Bearer "+careToken)).andExpect(status().isOk());
        cares.revoke(personal,relation.getId());
        mvc.perform(get(url).header("Authorization","Bearer "+careToken)).andExpect(status().isForbidden());
    }
}
