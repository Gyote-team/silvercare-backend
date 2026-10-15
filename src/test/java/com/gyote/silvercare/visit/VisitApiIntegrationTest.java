package com.gyote.silvercare.visit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.global.auth.application.JwtService;
import com.gyote.silvercare.health_record.command.application.HealthRecordCommandService;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.visit.domain.VisitStatus;
import com.gyote.silvercare.visit.domain.entity.Visit;
import com.gyote.silvercare.visit.domain.repository.VisitRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** 실제 인증 필터와 방문 HTTP 계약·권한·하위 자료 보존 회귀 검증. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisitApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired CareRelationCommandService cares;
    @Autowired PatientRepository patients;
    @Autowired VisitRepository visits;
    @Autowired HealthRecordCommandService records;
    @Autowired MedicalDocumentRepository documents;
    @Autowired EntityManager em;
    User personal, caregiver;
    UUID patientId;
    String token, caregiverToken;
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @BeforeEach
    void setup() {
        String key = UUID.randomUUID().toString();
        personal =
                accounts.chooseRole(
                        accounts.loginOrRegister("visit-personal-" + key, "개인").getKakaoId(),
                        UserRole.PATIENT);
        caregiver =
                accounts.chooseRole(
                        accounts.loginOrRegister("visit-caregiver-" + key, "보호자").getKakaoId(),
                        UserRole.CAREGIVER);
        patientId = patients.findByUserId(personal.getId()).orElseThrow().getId();
        token = jwt.create(personal);
        caregiverToken = jwt.create(caregiver);
    }

    String body(LocalDate date, String state) throws Exception {
        return json.writeValueAsString(
                Map.of(
                        "visitedOn",
                        date.toString(),
                        "hospitalName",
                        "병원",
                        "department",
                        "내과",
                        "memo",
                        "메모",
                        "visitStatus",
                        state,
                        "version",
                        0));
    }

    UUID create(LocalDate date, String state) throws Exception {
        var result =
                mvc.perform(
                                post("/api/visits")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType("application/json")
                                        .content(body(date, state)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                        .andExpect(jsonPath("$.documentIds").isArray())
                        .andExpect(jsonPath("$.healthRecordIds").isArray())
                        .andReturn();
        return UUID.fromString(
                json.readTree(result.getResponse().getContentAsString()).get("visitId").asText());
    }

    @Test
    void personalCrudCancelFilterAndIdempotency() throws Exception {
        UUID id = create(today, "DONE");
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body(today.minusDays(1), "DONE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitedOn").value(today.minusDays(1).toString()));
        for (int i = 0; i < 2; i++)
            mvc.perform(delete("/api/visits/" + id).header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        mvc.perform(get("/api/visits").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(
                        get("/api/visits?includeCanceled=true")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].visitStatus").value("CANCELED"));
        mvc.perform(get("/api/visits/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitStatus").value("CANCELED"));
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body(today, "DONE")))
                .andExpect(status().isConflict());
        assertThat(visits.findById(id).orElseThrow().getDeletedAt()).isNull();
    }

    @Test
    void futureDateRequiresScheduledAndCancelStillAllowed() throws Exception {
        for (String state : new String[] {"DONE"})
            mvc.perform(
                            post("/api/visits")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType("application/json")
                                    .content(body(today.plusDays(1), state)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VISIT_003"));
        UUID id = create(today.plusDays(1), "SCHEDULED");
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body(today.plusDays(1), "DONE")))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/visits/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void activeCaregiverManagesButRequestedAndRevokedCannot() throws Exception {
        UUID id = create(today, "DONE");
        var relation = cares.request(caregiver, accounts.patientInviteCode(personal));
        mvc.perform(get("/api/visits/" + id).header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isForbidden());
        cares.accept(personal, relation.getId());
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + caregiverToken)
                                .contentType("application/json")
                                .content(caregiverBody(0)))
                .andExpect(status().isOk());
        String input =
                json.writeValueAsString(
                        Map.of(
                                "patientId",
                                patientId,
                                "visitedOn",
                                today.toString(),
                                "visitStatus",
                                "DONE"));
        mvc.perform(
                        post("/api/visits")
                                .header("Authorization", "Bearer " + caregiverToken)
                                .contentType("application/json")
                                .content(input))
                .andExpect(status().isCreated());
        mvc.perform(
                        get("/api/visits?patientId=" + patientId)
                                .header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
        mvc.perform(get("/api/visits").header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isBadRequest());
        cares.revoke(personal, relation.getId());
        mvc.perform(get("/api/visits/" + id).header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + caregiverToken)
                                .contentType("application/json")
                                .content(body(today, "DONE")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/visits/" + id).header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void cursorSameDateAndScopeValidation() throws Exception {
        for (int i = 0; i < 3; i++) create(today, "DONE");
        HashSet<String> ids = new HashSet<>();
        String cursor = null;
        for (int i = 0; i < 3; i++) {
            var request =
                    get("/api/visits")
                            .param("size", "1")
                            .header("Authorization", "Bearer " + token);
            if (cursor != null) request.param("cursor", cursor);
            var response = mvc.perform(request);
            JsonNode page =
                    json.readTree(
                            response.andExpect(status().isOk())
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString());
            assertThat(ids.add(page.get("items").get(0).get("visitId").asText())).isTrue();
            cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asText();
        }
        assertThat(ids).hasSize(3);
        assertThat(cursor).isNull();
        String bound =
                json.readTree(
                                mvc.perform(
                                                get("/api/visits?size=1")
                                                        .header("Authorization", "Bearer " + token))
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .get("nextCursor")
                        .asText();
        mvc.perform(
                        get("/api/visits")
                                .param("cursor", bound)
                                .param("includeCanceled", "true")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        User other =
                accounts.chooseRole(
                        accounts.loginOrRegister("other-" + UUID.randomUUID(), "다른 개인")
                                .getKakaoId(),
                        UserRole.PATIENT);
        mvc.perform(
                        get("/api/visits")
                                .param("cursor", bound)
                                .header("Authorization", "Bearer " + jwt.create(other)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelPreservesReferencesAndDeletedChildrenAreExcluded() throws Exception {
        UUID id = create(today, "DONE");
        var record = records.create(personal, null, "연결 기록", id);
        var deleted = records.create(personal, null, "삭제된 기록", id);
        records.delete(personal, deleted.getId());
        var document =
                documents.save(
                        MedicalDocument.uploaded(
                                patientId,
                                id,
                                personal.getId(),
                                "test.pdf",
                                "test-key",
                                "application/pdf",
                                10,
                                null));
        em.flush();
        mvc.perform(delete("/api/visits/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/visits/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthRecordIds[0]").value(record.getId().toString()))
                .andExpect(jsonPath("$.healthRecordIds.length()").value(1))
                .andExpect(jsonPath("$.documentIds[0]").value(document.getId().toString()));
        assertThat(documents.findById(document.getId())).isPresent();
        document.delete(Instant.now());
        em.flush();
        mvc.perform(get("/api/visits/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentIds").isEmpty());
    }

    @Test
    void wrongTargetInvalidInputAndAuthAreRejected() throws Exception {
        UUID id = create(today, "DONE");
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "patientId",
                                                        UUID.randomUUID(),
                                                        "visitedOn",
                                                        today.toString(),
                                                        "visitStatus",
                                                        "DONE"))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/visits/" + id).header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/visits")).andExpect(status().isUnauthorized());
        for (String query :
                new String[] {
                    "size=0", "size=101", "size=no", "cursor=no", "includeCanceled=invalid"
                })
            mvc.perform(get("/api/visits?" + query).header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        mvc.perform(get("/api/visits/not-uuid").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        mvc.perform(
                        post("/api/visits")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body(today, "UNKNOWN")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/visits")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        get("/api/visits/" + UUID.randomUUID())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    String caregiverBody(long version) throws Exception {
        var input =
                (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(body(today, "DONE"));
        input.put("patientId", patientId.toString());
        input.put("version", version);
        return json.writeValueAsString(input);
    }

    @Test
    void canceledStatusCanOnlyBeSetByDelete() throws Exception {
        for (LocalDate date : new LocalDate[] {today.minusDays(1), today, today.plusDays(1)}) {
            mvc.perform(
                            post("/api/visits")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType("application/json")
                                    .content(body(date, "CANCELED")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        }
        UUID id = create(today, "DONE");
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body(today, "CANCELED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        mvc.perform(get("/api/visits/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.visitStatus").value("DONE"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void caregiverUpdateRequiresExplicitPatientAndVersion() throws Exception {
        UUID id = create(today, "DONE");
        var relation = cares.request(caregiver, accounts.patientInviteCode(personal));
        cares.accept(personal, relation.getId());
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + caregiverToken)
                                .contentType("application/json")
                                .content(body(today, "DONE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        for (String invalidVersion : new String[] {"null", "-1"}) {
            var input =
                    (com.fasterxml.jackson.databind.node.ObjectNode)
                            json.readTree(caregiverBody(0));
            input.set("version", json.readTree(invalidVersion));
            mvc.perform(
                            put("/api/visits/" + id)
                                    .header("Authorization", "Bearer " + caregiverToken)
                                    .contentType("application/json")
                                    .content(json.writeValueAsString(input)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        }
        var missing =
                (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(caregiverBody(0));
        missing.remove("version");
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + caregiverToken)
                                .contentType("application/json")
                                .content(json.writeValueAsString(missing)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staleScreenCannotOverwriteNewerPersonalOrCaregiverChange() throws Exception {
        UUID id = create(today, "DONE");
        var relation = cares.request(caregiver, accounts.patientInviteCode(personal));
        cares.accept(personal, relation.getId());
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body(today.minusDays(1), "DONE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + caregiverToken)
                                .contentType("application/json")
                                .content(caregiverBody(0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GLOBAL_004"));
        mvc.perform(get("/api/visits/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.visitedOn").value(today.minusDays(1).toString()))
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + caregiverToken)
                                .contentType("application/json")
                                .content(caregiverBody(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
        mvc.perform(
                        put("/api/visits/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(body(today, "DONE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GLOBAL_004"));
        mvc.perform(get("/api/visits").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items[0].version").value(2));
    }

    @Test
    void overlappingUpdatesUseOptimisticLock() throws Exception {
        UUID id = create(today, "DONE");
        Visit row = visits.findById(id).orElseThrow();
        // 별도 버전으로 이미 저장된 변경을 재현한다.
        em.createNativeQuery("update visits set version=version+1 where id=:id")
                .setParameter("id", id)
                .executeUpdate();
        row.update(today, "변경", "내과", null, VisitStatus.DONE);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> em.flush())
                .isInstanceOf(jakarta.persistence.OptimisticLockException.class);
    }
}
