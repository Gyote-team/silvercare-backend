package com.gyote.silvercare.medical_document.query;

import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.status.ResultStatus;
import com.gyote.silvercare.medical_document.api.dto.request.AiDocumentListRequestDto;
import com.gyote.silvercare.medical_document.domain.repository.AiDocumentExplanationRepository;
import com.gyote.silvercare.medical_document.domain.repository.AiDocumentRepository;
import com.gyote.silvercare.medical_document.query.application.AiDocumentQueryService;
import com.gyote.silvercare.medical_document.query.application.CareRelationPermissionService;
import com.gyote.silvercare.medical_document.query.model.AiDocumentDetailView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentExplanationStatusView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentListView;
import com.gyote.silvercare.medical_document.query.model.AiDocumentSectionsView;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.query.application.UserQueryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AiDocumentQueryServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository users;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private CareRelationRepository relations;

    @Autowired
    private AiDocumentRepository documents;

    @Autowired
    private AiDocumentExplanationRepository explanations;

    private User patient;
    private User caregiver;
    private Patient patientProfile;
    private UUID documentId;
    private AiDocumentQueryService queries;

    /** 테스트 실행 전에 문서 조회에 필요한 사용자와 샘플 데이터를 준비한다. */
    @BeforeEach
    void setUp() {
        entityManager.createNativeQuery("""
                CREATE TABLE IF NOT EXISTS visits (
                    id UUID PRIMARY KEY,
                    patient_id UUID NOT NULL,
                    visited_on DATE NOT NULL
                )
                """).executeUpdate();

        UserAccountService accounts = new UserAccountService(users, patients);
        patient = accounts.chooseRole(
                accounts.loginOrRegister("patient-issue-3", "환자 테스트").getKakaoId(),
                UserRole.PATIENT
        );
        caregiver = accounts.chooseRole(
                accounts.loginOrRegister("caregiver-issue-3", "보호자 테스트").getKakaoId(),
                UserRole.CAREGIVER
        );
        patientProfile = patients.findByUserId(patient.getId()).orElseThrow();

        CareRelationCommandService careCommands = new CareRelationCommandService(relations, patients);
        CareRelation requested = careCommands.request(caregiver, accounts.patientInviteCode(patient));
        careCommands.accept(patient, requested.getId());

        documentId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID analysisId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID explanationId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID citationId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-22T01:00:00Z");

        execute("INSERT INTO visits (id, patient_id, visited_on) VALUES (:id, :patientId, :visitedOn)",
                "id", visitId, "patientId", patientProfile.getId(), "visitedOn", LocalDate.of(2026, 9, 20));
        execute("""
                INSERT INTO documents
                    (id, visit_id, uploader_user_id, storage_key, file_name, mime_type,
                     file_size_bytes, document_type, status, created_at, updated_at)
                VALUES
                    (:id, :visitId, :uploaderId, :storageKey, :fileName, :mimeType,
                     :fileSize, :documentType, :status, :createdAt, :updatedAt)
                """,
                "id", documentId,
                "visitId", visitId,
                "uploaderId", patient.getId(),
                "storageKey", "documents/issue-3.pdf",
                "fileName", "혈액검사 결과지",
                "mimeType", "application/pdf",
                "fileSize", 1024L,
                "documentType", "LAB_RESULT",
                "status", "READY",
                "createdAt", createdAt,
                "updatedAt", createdAt);
        execute("""
                INSERT INTO document_analyses
                    (id, document_id, parser_version, status, created_at, completed_at)
                VALUES (:id, :documentId, :parserVersion, :status, :createdAt, :completedAt)
                """,
                "id", analysisId,
                "documentId", documentId,
                "parserVersion", "test-parser",
                "status", "SUCCEEDED",
                "createdAt", createdAt,
                "completedAt", createdAt);
        execute("""
                INSERT INTO ai_runs
                    (id, analysis_id, run_type, model_name, prompt_version, status,
                     current_step, progress, retryable, retry_count, completed_at, created_at)
                VALUES (:id, :analysisId, :runType, :modelName, :promptVersion, :status,
                        :currentStep, :progress, :retryable, :retryCount, :completedAt, :createdAt)
                """,
                "id", runId,
                "analysisId", analysisId,
                "runType", "EXPLANATION",
                "modelName", "test-model",
                "promptVersion", "v1",
                "status", "SUCCEEDED",
                "currentStep", "COMPLETED",
                "progress", 100,
                "retryable", false,
                "retryCount", 0,
                "completedAt", createdAt,
                "createdAt", createdAt);
        execute("""
                INSERT INTO ai_explanations
                    (id, document_id, ai_run_id, version, title, content, result_status, created_at, completed_at)
                VALUES (:id, :documentId, :runId, :version, :title, :content, :resultStatus, :createdAt, :completedAt)
                """,
                "id", explanationId,
                "documentId", documentId,
                "runId", runId,
                "version", 1,
                "title", "검사 결과 쉬운 설명",
                "content", "검사 결과를 쉽게 정리한 내용입니다.",
                "resultStatus", "COMPLETE",
                "createdAt", createdAt,
                "completedAt", createdAt);
        execute("""
                INSERT INTO explanation_sections
                    (id, explanation_id, section_type, title, section_order, created_at)
                VALUES (:id, :explanationId, :sectionType, :title, :sectionOrder, :createdAt)
                """,
                "id", sectionId,
                "explanationId", explanationId,
                "sectionType", "LAB_RESULT",
                "title", "검사 결과",
                "sectionOrder", 1,
                "createdAt", createdAt);
        execute("""
                INSERT INTO explanation_items
                    (id, section_id, label, display_value, unit, has_source, item_order, created_at)
                VALUES (:id, :sectionId, :label, :displayValue, :unit, :hasSource, :itemOrder, :createdAt)
                """,
                "id", itemId,
                "sectionId", sectionId,
                "label", "HbA1c",
                "displayValue", "6.5",
                "unit", "%",
                "hasSource", true,
                "itemOrder", 1,
                "createdAt", createdAt);
        execute("""
                INSERT INTO explanation_citations
                    (id, explanation_id, explanation_item_id, quoted_text, created_at)
                VALUES (:id, :explanationId, :itemId, :quotedText, :createdAt)
                """,
                "id", citationId,
                "explanationId", explanationId,
                "itemId", itemId,
                "quotedText", "HbA1c 6.5%",
                "createdAt", createdAt);

        entityManager.flush();
        queries = new AiDocumentQueryService(
                documents,
                explanations,
                new CareRelationPermissionService(patients, relations),
                new UserQueryService(users)
        );
    }

    /** 환자와 활성 보호자가 문서 설명 조회 기능을 사용할 수 있는지 검증한다. */
    @Test
    void patientAndActiveCaregiverCanReadDocumentExplanation() {
        AiDocumentListRequestDto request = new AiDocumentListRequestDto(null, null, null, null, 0, 20);

        Page<AiDocumentListView> patientDocuments = queries.listDocuments(patient, request);
        assertThat(patientDocuments.getTotalElements()).isEqualTo(1);
        assertThat(patientDocuments.getContent().get(0).documentId()).isEqualTo(documentId);
        assertThat(patientDocuments.getContent().get(0).jobStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
        assertThat(patientDocuments.getContent().get(0).resultStatus()).isEqualTo(ResultStatus.COMPLETE);

        AiDocumentDetailView detail = queries.getDocument(patient, documentId);
        assertThat(detail.title()).isEqualTo("검사 결과 쉬운 설명");
        assertThat(detail.sectionCount()).isEqualTo(1);
        assertThat(detail.citationCount()).isEqualTo(1);

        AiDocumentSectionsView sections = queries.getSections(patient, documentId, null);
        assertThat(sections.sections()).hasSize(1);
        assertThat(sections.sections().get(0).items()).hasSize(1);

        AiDocumentExplanationStatusView status = queries.getExplanationStatus(caregiver, documentId);
        assertThat(status.jobStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
        assertThat(status.resultStatus()).isEqualTo(ResultStatus.COMPLETE);
        assertThat(status.detailUrl()).isEqualTo("/api/ai-documents/" + documentId);
    }

    /** 활성 돌봄 관계가 없는 보호자의 문서 조회를 차단하는지 검증한다. */
    @Test
    void caregiverWithoutActiveRelationCannotReadDocument() {
        CareRelation relation = relations.findByPatientIdOrderByRequestedAtDesc(patientProfile.getId())
                .stream()
                .findFirst()
                .orElseThrow();
        relation.setStatus(com.gyote.silvercare.care_relation.domain.CareRelationStatus.REVOKED);
        relations.saveAndFlush(relation);

        assertThatThrownBy(() -> queries.getExplanationStatus(caregiver, documentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("권한");
    }

    /** 테스트 SQL을 named parameter와 함께 실행한다. */
    private void execute(String sql, Object... parameters) {
        var query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < parameters.length; i += 2) {
            query.setParameter(String.valueOf(parameters[i]), parameters[i + 1]);
        }
        query.executeUpdate();
    }
}
