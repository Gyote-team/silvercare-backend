package com.gyote.silvercare.action_item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyote.silvercare.action_item.query.application.ActionItemQueryService;
import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.global.auth.application.JwtService;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.command.application.MedicalDocumentCommandService;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.visit.domain.VisitStatus;
import com.gyote.silvercare.visit.domain.entity.Visit;
import com.gyote.silvercare.visit.domain.repository.VisitRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 인증부터 실제 후보 SQL·근거 필터·문서 삭제 연동까지 확인하는 회귀 테스트다. */
@SpringBootTest(
        properties =
                "spring.datasource.url=jdbc:h2:mem:action-items-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class ActionItemQueryApiTest {
    private static final Instant CREATED_AT = Instant.parse("2026-11-27T00:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired PatientRepository patients;
    @Autowired VisitRepository visits;
    @Autowired MedicalDocumentRepository documents;
    @Autowired MedicalDocumentCommandService documentCommands;
    @Autowired CareRelationCommandService relations;
    @Autowired ActionItemQueryService queries;
    @Autowired JdbcTemplate jdbc;

    private User personal;
    private User caregiver;
    private User other;
    private UUID patientId;
    private UUID otherPatientId;
    private MedicalDocument document;

    @BeforeEach
    void setup() {
        // 이 테이블들은 AI가 작성하며 Spring의 H2 엔티티 자동 생성 대상에 포함되지 않는다.
        jdbc.execute(
                """
                CREATE TABLE IF NOT EXISTS document_pages (
                    id UUID PRIMARY KEY, document_id UUID NOT NULL, page_no INTEGER NOT NULL)
                """);
        jdbc.execute(
                """
CREATE TABLE IF NOT EXISTS extracted_items (
    id UUID PRIMARY KEY, analysis_id UUID NOT NULL, page_id UUID, source_text TEXT NOT NULL)
""");
        personal = user(UserRole.PATIENT);
        caregiver = user(UserRole.CAREGIVER);
        other = user(UserRole.PATIENT);
        patientId = patients.findByUserId(personal.getId()).orElseThrow().getId();
        otherPatientId = patients.findByUserId(other.getId()).orElseThrow().getId();
        document = document(patientId, personal);
    }

    @Test
    void emptyListAndLegacyCandidateHaveNoInventedEvidence() throws Exception {
        perform(personal, get("/api/action-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").isEmpty());
        UUID id = candidate(document, "PENDING");
        Map<String, Object> before = jdbc.queryForMap("select * from action_items where id=?", id);
        perform(personal, get("/api/action-items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.documentName").value("가상 진료 문서.pdf"))
                .andExpect(jsonPath("$.title").value("가상 후보"))
                .andExpect(jsonPath("$.estimatedAt").isEmpty())
                .andExpect(jsonPath("$.sourceItemId").isEmpty())
                .andExpect(jsonPath("$.confidence").isEmpty())
                .andExpect(jsonPath("$.source").isEmpty())
                .andExpect(jsonPath("$.sourceExcerpt").isEmpty());
        perform(personal, get("/api/action-items")).andExpect(status().isOk());
        assertThat(jdbc.queryForMap("select * from action_items where id=?", id)).isEqualTo(before);
    }

    @Test
    void detailReturnsOnlyExplicitSourceAndCandidateConfidence() throws Exception {
        UUID id = candidate(document, "PENDING");
        UUID source = evidence(document, "SUCCEEDED", document);
        jdbc.update(
                "update action_items set source_item_id=?, confidence=?, due_at=? where id=?",
                source,
                0.875,
                Timestamp.from(CREATED_AT.plusSeconds(86400)),
                id);
        perform(personal, get("/api/action-items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceItemId").value(source.toString()))
                .andExpect(jsonPath("$.source.pageNo").value(2))
                .andExpect(jsonPath("$.source.sourceText").value("가상 원문: 다음 진료 때 확인"))
                .andExpect(jsonPath("$.sourceExcerpt").value("가상 원문: 다음 진료 때 확인"))
                .andExpect(jsonPath("$.confidence").value(0.875))
                .andExpect(jsonPath("$.estimatedAt").value("2026-11-28T00:00:00Z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OTHER_DOCUMENT", "OTHER_PAGE", "RUNNING"})
    void mismatchedOrUnfinishedEvidenceDoesNotLeak(String kind) throws Exception {
        MedicalDocument foreign = document(otherPatientId, other);
        UUID source =
                evidence(
                        "OTHER_DOCUMENT".equals(kind) ? foreign : document,
                        "RUNNING".equals(kind) ? "RUNNING" : "SUCCEEDED",
                        "OTHER_PAGE".equals(kind) ? foreign : document);
        UUID id = candidate(document, "PENDING");
        jdbc.update("update action_items set source_item_id=? where id=?", source, id);
        perform(personal, get("/api/action-items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").isEmpty())
                .andExpect(jsonPath("$.sourceItemId").isEmpty())
                .andExpect(jsonPath("$.sourceExcerpt").isEmpty());
    }

    @Test
    void cursorPaginatesEqualTimestampsAndIsBoundToStatus() throws Exception {
        Set<String> expected = new HashSet<>();
        for (int i = 0; i < 3; i++) expected.add(candidate(document, "PENDING").toString());
        candidate(document, "REJECTED");
        Set<String> found = new HashSet<>();
        String cursor = null;
        for (int i = 0; i < 3; i++) {
            var request =
                    get("/api/action-items").param("approvalStatus", "PENDING").param("size", "1");
            if (cursor != null) request.param("cursor", cursor);
            JsonNode page = body(perform(personal, request).andExpect(status().isOk()));
            assertThat(page.path("items").size()).isEqualTo(1);
            assertThat(found.add(page.path("items").get(0).path("actionItemId").asText())).isTrue();
            cursor = page.path("nextCursor").isNull() ? null : page.path("nextCursor").asText();
            if (i == 0) {
                perform(
                                personal,
                                get("/api/action-items")
                                        .param("cursor", cursor)
                                        .param("approvalStatus", "REJECTED"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("GLOBAL_002"));
            }
        }
        assertThat(found).isEqualTo(expected);
        assertThat(cursor).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "APPROVED", "REJECTED", "CANCELED"})
    void statusFilterIncludesExactlyTheRequestedState(String approvalStatus) throws Exception {
        for (String state : new String[] {"PENDING", "APPROVED", "REJECTED", "CANCELED"})
            candidate(document, state);
        JsonNode page =
                body(
                        perform(
                                        personal,
                                        get("/api/action-items")
                                                .param("approvalStatus", approvalStatus))
                                .andExpect(status().isOk()));
        assertThat(page.path("items").size()).isEqualTo(1);
        assertThat(page.path("items").get(0).path("approvalStatus").asText())
                .isEqualTo(approvalStatus);
    }

    @Test
    void otherPersonalAndRequestedOrRevokedGuardianCannotRead() throws Exception {
        UUID id = candidate(document, "PENDING");
        perform(other, get("/api/action-items").param("patientId", patientId.toString()))
                .andExpect(status().isForbidden());
        perform(other, get("/api/action-items/" + id)).andExpect(status().isForbidden());
        mvc.perform(get("/api/action-items")).andExpect(status().isUnauthorized());
        perform(caregiver, get("/api/action-items")).andExpect(status().isBadRequest());
        var relation = relations.request(caregiver, accounts.patientInviteCode(personal));
        perform(caregiver, get("/api/action-items/" + id)).andExpect(status().isForbidden());
        relations.accept(personal, relation.getId());
        perform(caregiver, get("/api/action-items/" + id)).andExpect(status().isOk());
        perform(caregiver, get("/api/action-items").param("patientId", patientId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
        relations.revoke(personal, relation.getId());
        perform(caregiver, get("/api/action-items/" + id)).andExpect(status().isForbidden());
        perform(caregiver, get("/api/action-items").param("patientId", patientId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cursorCannotBeReusedForAnotherAccessiblePatient() throws Exception {
        candidate(document, "PENDING");
        candidate(document, "PENDING");
        var first = relations.request(caregiver, accounts.patientInviteCode(personal));
        var second = relations.request(caregiver, accounts.patientInviteCode(other));
        relations.accept(personal, first.getId());
        relations.accept(other, second.getId());
        String cursor =
                body(perform(
                                        caregiver,
                                        get("/api/action-items")
                                                .param("patientId", patientId.toString())
                                                .param("size", "1"))
                                .andExpect(status().isOk()))
                        .path("nextCursor")
                        .asText();
        perform(
                        caregiver,
                        get("/api/action-items")
                                .param("patientId", otherPatientId.toString())
                                .param("cursor", cursor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualDocumentDeletionCancelsPendingAndHidesEvidenceWhilePreservingApproved()
            throws Exception {
        UUID source = evidence(document, "SUCCEEDED", document);
        UUID pending = candidate(document, "PENDING");
        UUID approved = candidate(document, "APPROVED");
        jdbc.update(
                "update action_items set source_item_id=? where document_id=?",
                source,
                document.getId());
        documentCommands.delete(personal, document.getId());
        perform(personal, get("/api/action-items/" + pending))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("CANCELED"))
                .andExpect(jsonPath("$.cancelReason").value("SOURCE_DOCUMENT_DELETED"))
                .andExpect(jsonPath("$.sourceDocumentDeleted").value(true))
                .andExpect(jsonPath("$.documentId").value(document.getId().toString()))
                .andExpect(jsonPath("$.documentName").isEmpty())
                .andExpect(jsonPath("$.sourceItemId").isEmpty())
                .andExpect(jsonPath("$.source").isEmpty());
        perform(personal, get("/api/action-items/" + approved))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
                .andExpect(jsonPath("$.cancelReason").isEmpty())
                .andExpect(jsonPath("$.sourceDocumentDeleted").value(true))
                .andExpect(jsonPath("$.sourceExcerpt").isEmpty());
        perform(personal, get("/api/action-items").param("approvalStatus", "CANCELED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void invalidDocumentVisitOwnershipAndDeletedVisitsAreExcluded() throws Exception {
        UUID valid = candidate(document, "PENDING");
        UUID mismatch = candidate(document, "PENDING");
        var foreign = document(otherPatientId, other);
        jdbc.update(
                "update action_items set visit_id=? where id=?", foreign.getVisitId(), mismatch);
        perform(personal, get("/api/action-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
        perform(personal, get("/api/action-items/" + mismatch)).andExpect(status().isNotFound());
        jdbc.update(
                "update visits set deleted_at=? where id=?",
                Timestamp.from(CREATED_AT),
                document.getVisitId());
        perform(personal, get("/api/action-items/" + valid)).andExpect(status().isNotFound());
        perform(personal, get("/api/action-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void invalidQueriesUseGlobalInputErrorsAndMissingCandidateIsNotFound() throws Exception {
        for (String size : new String[] {"0", "101", "abc"}) {
            perform(personal, get("/api/action-items").param("size", size))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        }
        perform(personal, get("/api/action-items").param("approvalStatus", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        perform(personal, get("/api/action-items").param("cursor", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        perform(personal, get("/api/action-items/not-a-uuid")).andExpect(status().isBadRequest());
        perform(personal, get("/api/action-items/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACTION_ITEM_001"));
    }

    @Test
    void inactiveActorIsDeniedEvenWhenOwnershipMatches() {
        UUID id = candidate(document, "PENDING");
        personal.setStatus(UserStatus.WITHDRAWN);
        assertThatThrownBy(() -> queries.detail(personal, id))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> queries.list(personal, patientId, null, null, 20))
                .isInstanceOf(BusinessException.class);
    }

    private User user(UserRole role) {
        User registered = accounts.loginOrRegister("action-query-" + UUID.randomUUID(), "가상 사용자");
        return accounts.chooseRole(registered.getKakaoId(), role);
    }

    private MedicalDocument document(UUID patient, User uploader) {
        Visit visit =
                visits.saveAndFlush(
                        Visit.create(
                                patient,
                                LocalDate.of(2026, 11, 27),
                                "가상 병원",
                                "내과",
                                null,
                                VisitStatus.SCHEDULED));
        return documents.saveAndFlush(
                MedicalDocument.uploaded(
                        patient,
                        visit.getId(),
                        uploader.getId(),
                        "가상 진료 문서.pdf",
                        "synthetic/file.pdf",
                        "application/pdf",
                        10,
                        "synthetic-request"));
    }

    private UUID candidate(MedicalDocument source, String state) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
insert into action_items(id,visit_id,document_id,type,title,description,approval_status,created_at)
values (?,?,?,'REVISIT','가상 후보','가상 설명',?,?)
""",
                id,
                source.getVisitId(),
                source.getId(),
                state,
                Timestamp.from(CREATED_AT));
        return id;
    }

    private UUID evidence(
            MedicalDocument source, String analysisStatus, MedicalDocument pageDocument) {
        UUID analysisId = UUID.randomUUID();
        UUID pageId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        jdbc.update(
                "insert into document_analyses(id,document_id,parser_version,status,created_at)"
                    + " values (?,?,?, ?,?)",
                analysisId,
                source.getId(),
                "synthetic",
                analysisStatus,
                Timestamp.from(CREATED_AT));
        jdbc.update(
                "insert into document_pages(id,document_id,page_no) values (?,?,2)",
                pageId,
                pageDocument.getId());
        jdbc.update(
                "insert into extracted_items(id,analysis_id,page_id,source_text) values (?,?,?,?)",
                itemId,
                analysisId,
                pageId,
                "가상 원문: 다음 진료 때 확인");
        return itemId;
    }

    private ResultActions perform(User actor, MockHttpServletRequestBuilder request)
            throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + jwt.create(actor)));
    }

    private JsonNode body(ResultActions response) throws Exception {
        return json.readTree(response.andReturn().getResponse().getContentAsString());
    }
}
