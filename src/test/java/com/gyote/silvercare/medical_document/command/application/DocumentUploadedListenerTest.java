package com.gyote.silvercare.medical_document.command.application;

import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.medical_document.domain.AnalysisFailureType;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisPort;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisRequest;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisResult;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisStatus;
import com.gyote.silvercare.medical_document.domain.DocumentStoragePort;
import com.gyote.silvercare.medical_document.domain.DocumentUploadedEvent;
import com.gyote.silvercare.medical_document.domain.MedicalDocumentAccessPolicy;
import com.gyote.silvercare.medical_document.domain.entity.DocumentAnalysis;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.repository.DocumentAnalysisRepository;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import com.gyote.silvercare.medical_document.query.application.MedicalDocumentQueryService;
import com.gyote.silvercare.medical_document.query.model.MedicalDocumentView;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.query.application.UserQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** 업로드 이벤트 리스너가 분석 시작을 요청하고, 재시도·최종 실패 상태를 DB에 기록하는지 확인하는 테스트입니다. */
@DataJpaTest
class DocumentUploadedListenerTest {

    private static final DocumentAnalysisResult SUCCESS = DocumentAnalysisResult.success();
    private static final DocumentAnalysisResult UNAVAILABLE =
            DocumentAnalysisResult.failure(AnalysisFailureType.AI_UNAVAILABLE);
    private static final DocumentAnalysisResult AUTH_FAILED =
            DocumentAnalysisResult.failure(AnalysisFailureType.INTERNAL_AUTH_FAILED);
    private static final DocumentAnalysisResult TIMEOUT =
            DocumentAnalysisResult.failure(AnalysisFailureType.AI_TIMEOUT);

    @Autowired
    private UserRepository users;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private CareRelationRepository relations;

    @Autowired
    private MedicalDocumentRepository documents;

    @Autowired
    private DocumentAnalysisRepository analyses;

    @Autowired
    private TestEntityManager em;

    private FakeAnalysisPort port;
    private DocumentAnalysisStateCommandService states;
    private DocumentUploadedListener listener;
    private MedicalDocumentQueryService queries;

    private User patient;
    private MedicalDocument document;
    private DocumentAnalysis analysis;

    @BeforeEach
    void setUp() {
        em.getEntityManager().createNativeQuery("""
                CREATE TABLE IF NOT EXISTS visits (
                    id UUID PRIMARY KEY,
                    patient_id UUID NOT NULL,
                    visited_on DATE NOT NULL
                )
                """).executeUpdate();
        port = new FakeAnalysisPort();
        states = new DocumentAnalysisStateCommandService(documents, analyses);
        listener = new DocumentUploadedListener(documents, port, states, 3, Duration.ZERO);
        queries = new MedicalDocumentQueryService(
                documents, users, new UserQueryService(users), patients,
                new MedicalDocumentAccessPolicy(relations, patients), new StubStorage());

        patient = createPatient();
        document = saveUploadedDocument();
        analysis = analyses.saveAndFlush(DocumentAnalysis.queued(document.getId(), "test-parser"));
    }

    @Test
    void successOnFirstAttemptKeepsUploadedAndPending() {
        port.respondWith(SUCCESS);

        handleUploadedEvent();

        assertThat(port.requests).hasSize(1);
        assertStillQueued(0);
    }

    @Test
    void successAfterTwoFailuresRecordsTwoRetries() {
        port.respondWith(UNAVAILABLE, UNAVAILABLE, SUCCESS);

        handleUploadedEvent();

        assertThat(port.requests).hasSize(3);
        assertStillQueued(2);
    }

    @Test
    void retryableFailureOnEveryAttemptIsFinalFailure() {
        port.respondWith(UNAVAILABLE, UNAVAILABLE, UNAVAILABLE);

        handleUploadedEvent();

        assertThat(port.requests).hasSize(3);
        assertFinalFailure(AnalysisFailureType.AI_UNAVAILABLE, true, 2);
    }

    @Test
    void authFailureIsFinalFailureWithoutRetry() {
        port.respondWith(AUTH_FAILED);

        handleUploadedEvent();

        assertThat(port.requests).hasSize(1);
        assertFinalFailure(AnalysisFailureType.INTERNAL_AUTH_FAILED, false, 0);
    }

    @Test
    void authFailureAfterRetryableFailureIsFinalFailure() {
        port.respondWith(UNAVAILABLE, AUTH_FAILED);

        handleUploadedEvent();

        assertThat(port.requests).hasSize(2);
        assertFinalFailure(AnalysisFailureType.INTERNAL_AUTH_FAILED, false, 1);
    }

    @Test
    void documentDeletedWhileWaitingIsNotRequestedAgain() {
        port.respondWith(UNAVAILABLE, SUCCESS);
        port.afterRequest = () -> document.delete(Instant.now());

        handleUploadedEvent();

        assertThat(port.requests).hasSize(1);
        assertThat(reloadDocument().getStatus()).isEqualTo(DocumentStatus.DELETED);
        assertThat(reloadAnalysis().getStatus()).isEqualTo(DocumentAnalysisStatus.PENDING);
    }

    @Test
    void changeToFailedKeepsDeletedDocumentAndDeletedAt() {
        document.delete(Instant.now());
        em.flush();

        states.changeToFailed(document.getId(), analysis.getId(), AnalysisFailureType.AI_UNAVAILABLE);
        em.clear();

        MedicalDocument reloaded = reloadDocument();
        assertThat(reloaded.getStatus()).isEqualTo(DocumentStatus.DELETED);
        assertThat(reloaded.getDeletedAt()).isNotNull();
        assertThat(reloadAnalysis().getStatus()).isEqualTo(DocumentAnalysisStatus.FAILED);
    }

    @Test
    void deletedDocumentIsNotRequested() {
        document.delete(Instant.now());
        em.flush();

        handleUploadedEvent();

        assertThat(port.requests).isEmpty();
        assertThat(reloadAnalysis().getStatus()).isEqualTo(DocumentAnalysisStatus.PENDING);
    }

    @Test
    void missingDocumentIsIgnoredWithoutException() {
        DocumentUploadedEvent unknown = new DocumentUploadedEvent(UUID.randomUUID(), UUID.randomUUID());

        assertThatCode(() -> listener.requestAnalysis(unknown)).doesNotThrowAnyException();

        assertThat(port.requests).isEmpty();
    }

    @Test
    void portExceptionDoesNotEscapeListener() {
        port.throwOnRequest = true;

        assertThatCode(this::handleUploadedEvent).doesNotThrowAnyException();

        assertFinalFailure(AnalysisFailureType.AI_UNAVAILABLE, true, 0);
    }

    @Test
    void failureWhileRecordingUnexpectedFailureDoesNotEscapeListener() {
        port.throwOnRequest = true;
        listener = new DocumentUploadedListener(documents, port, statesFailingToRecord(), 3, Duration.ZERO);

        assertThatCode(this::handleUploadedEvent).doesNotThrowAnyException();

        assertStillQueued(0);
    }

    @Test
    void requestCarriesStorageKeyMimeTypeAndRequestIdOfDocument() {
        handleUploadedEvent();

        assertThat(port.requests).singleElement().isEqualTo(new DocumentAnalysisRequest(
                document.getId(), document.getStorageKey(), document.getMimeType(), document.getRequestId()));
    }

    @Test
    void queryShowsUploadedAndQueuedRightAfterUpload() {
        handleUploadedEvent();

        MedicalDocumentView view = queries.get(patient, document.getId());

        assertThat(view.documentStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(view.latestAiJobStatus()).isEqualTo(AiJobStatus.QUEUED);
    }

    @Test
    void queryShowsFailedAfterFinalFailure() {
        port.respondWith(AUTH_FAILED);

        handleUploadedEvent();

        MedicalDocumentView view = queries.get(patient, document.getId());

        assertThat(view.documentStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(view.latestAiJobStatus()).isEqualTo(AiJobStatus.FAILED);
    }

    @Test
    void queryShowsRetryableAfterTimeoutFailure() {
        port.respondWith(TIMEOUT, TIMEOUT, TIMEOUT);

        handleUploadedEvent();

        assertThat(queries.get(patient, document.getId()).retryable()).isTrue();
    }

    @Test
    void queryShowsNotRetryableAfterAuthFailure() {
        port.respondWith(AUTH_FAILED);

        handleUploadedEvent();

        assertThat(queries.get(patient, document.getId()).retryable()).isFalse();
    }

    /** 리스너 메서드를 직접 호출한 뒤, 이후 조회가 DB 값을 읽도록 flush·clear 합니다. */
    private void handleUploadedEvent() {
        listener.requestAnalysis(new DocumentUploadedEvent(document.getId(), analysis.getId()));
        em.flush();
        em.clear();
    }

    private void assertStillQueued(int retryCount) {
        assertThat(reloadDocument().getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        DocumentAnalysis reloaded = reloadAnalysis();
        assertThat(reloaded.getStatus()).isEqualTo(DocumentAnalysisStatus.PENDING);
        assertThat(reloaded.getRetryCount()).isEqualTo(retryCount);
    }

    private void assertFinalFailure(AnalysisFailureType type, boolean retryable, int retryCount) {
        assertThat(reloadDocument().getStatus()).isEqualTo(DocumentStatus.FAILED);
        DocumentAnalysis reloaded = reloadAnalysis();
        assertThat(reloaded.getStatus()).isEqualTo(DocumentAnalysisStatus.FAILED);
        assertThat(reloaded.getErrorCode()).isEqualTo(type.name());
        assertThat(reloaded.getFailedStep()).isEqualTo("ANALYSIS_REQUEST");
        assertThat(reloaded.isRetryable()).isEqualTo(retryable);
        assertThat(reloaded.getRetryCount()).isEqualTo(retryCount);
        assertThat(reloaded.getCompletedAt()).isNotNull();
    }

    /** 최종 실패 기록이 항상 예외로 끝나는 상태 기록 서비스를 만듭니다. */
    private DocumentAnalysisStateCommandService statesFailingToRecord() {
        return new DocumentAnalysisStateCommandService(documents, analyses) {
            @Override
            public void changeToFailed(UUID documentId, UUID analysisId, AnalysisFailureType failureType) {
                throw new IllegalStateException("기록 오류");
            }
        };
    }

    private MedicalDocument reloadDocument() {
        return documents.findById(document.getId()).orElseThrow();
    }

    private DocumentAnalysis reloadAnalysis() {
        return analyses.findById(analysis.getId()).orElseThrow();
    }

    private User createPatient() {
        UserAccountService accounts = new UserAccountService(users, patients);
        return accounts.chooseRole(accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(), UserRole.PATIENT);
    }

    private MedicalDocument saveUploadedDocument() {
        UUID id = UUID.randomUUID();
        UUID patientId = patients.findByUserId(patient.getId()).orElseThrow().getId();
        return documents.saveAndFlush(MedicalDocument.uploadedWithId(
                id, patientId, UUID.randomUUID(), patient.getId(),
                "검사결과.pdf", "documents/" + id + "/original.pdf", "application/pdf", 1024L,
                "upload-20261003-abcd1234", "listener-key", null));
    }

    /**
     * 미리 정한 결과를 차례로 돌려주고 받은 요청을 기록하는 테스트용 포트입니다. 정한 결과가 없으면 성공을 돌려줍니다.
     * afterRequest는 요청을 받은 직후 실행되어, 재시도 대기 중에 일어나는 일(문서 삭제 등)을 흉내 냅니다.
     */
    private static class FakeAnalysisPort implements DocumentAnalysisPort {

        private final Deque<DocumentAnalysisResult> results = new ArrayDeque<>();
        private final List<DocumentAnalysisRequest> requests = new ArrayList<>();
        private boolean throwOnRequest;
        private Runnable afterRequest = () -> { };

        void respondWith(DocumentAnalysisResult... planned) {
            results.addAll(List.of(planned));
        }

        @Override
        public DocumentAnalysisResult requestAnalysis(DocumentAnalysisRequest request) {
            requests.add(request);
            afterRequest.run();
            if (throwOnRequest) {
                throw new IllegalStateException("포트 오류");
            }
            return results.isEmpty() ? SUCCESS : results.poll();
        }
    }

    /** 조회 서비스가 서명 URL을 만들 때만 쓰는 테스트용 저장소입니다. */
    private static class StubStorage implements DocumentStoragePort {

        @Override
        public String createSignedUrl(String storageKey, Duration ttl) {
            return "https://storage.test/" + storageKey;
        }

        @Override
        public void storeOriginal(String storageKey, byte[] content, String mimeType) {
        }

        @Override
        public void deleteOriginal(String storageKey) {
        }
    }
}
