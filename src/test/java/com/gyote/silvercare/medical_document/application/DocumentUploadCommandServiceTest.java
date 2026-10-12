package com.gyote.silvercare.medical_document.application;

import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.global.type.DocumentType;
import com.gyote.silvercare.medical_document.command.application.DocumentUploadCommand;
import com.gyote.silvercare.medical_document.command.application.DocumentUploadCommandService;
import com.gyote.silvercare.medical_document.command.application.DocumentUploadResult;
import com.gyote.silvercare.medical_document.command.application.DocumentUploadSaveCommandService;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisStatus;
import com.gyote.silvercare.medical_document.domain.DocumentFileValidator;
import com.gyote.silvercare.medical_document.domain.DocumentStorageException;
import com.gyote.silvercare.medical_document.domain.DocumentStoragePort;
import com.gyote.silvercare.medical_document.domain.DocumentUploadedEvent;
import com.gyote.silvercare.medical_document.domain.MedicalDocumentAccessPolicy;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.repository.DocumentAnalysisRepository;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 문서 업로드 서비스가 권한·멱등 처리·원본 저장·보상 삭제 규칙대로 동작하는지 확인하는 테스트입니다. */
@DataJpaTest
class DocumentUploadCommandServiceTest {

    private static final String KEY = "upload-key-1";
    private static final String PDF_MIME = "application/pdf";
    private static final byte[] PDF = pdf();

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

    private UserAccountService accounts;
    private RecordingStorage storage;
    private List<Object> publishedEvents;
    private DocumentUploadCommandService uploads;

    private User patient;
    private User caregiver;
    private User strangerCaregiver;
    private UUID patientId;
    private UUID visitId;
    private MedicalDocument winner;

    @BeforeEach
    void setUp() {
        execute("""
                CREATE TABLE IF NOT EXISTS visits (
                    id UUID PRIMARY KEY,
                    patient_id UUID NOT NULL,
                    visited_on DATE NOT NULL
                )
                """);
        // 다른 테스트가 deleted_at 없이 visits를 먼저 만들었을 수 있어 컬럼을 따로 보탭니다.
        execute("ALTER TABLE visits ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP");
        accounts = new UserAccountService(users, patients);
        storage = new RecordingStorage();
        publishedEvents = new ArrayList<>();
        uploads = uploadService(newSaver());

        patient = createUser("kakao-soonja", "김순자", UserRole.PATIENT);
        caregiver = createUser("kakao-minji", "김민지", UserRole.CAREGIVER);
        strangerCaregiver = createUser("kakao-stranger", "남보호", UserRole.CAREGIVER);
        connect(caregiver, patient);
        patientId = patients.findByUserId(patient.getId()).orElseThrow().getId();
        visitId = saveVisit();
    }

    @Test
    void patientUploadSavesDocumentAnalysisOriginalAndEvent() {
        DocumentUploadResult result = uploads.upload(patient, command(visitId, KEY, "LAB_RESULT"));

        MedicalDocument document = result.document();
        assertThat(result.created()).isTrue();
        assertThat(result.latestAiJobStatus()).isEqualTo(AiJobStatus.QUEUED);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(document.getStorageKey()).isEqualTo("documents/" + document.getId() + "/original.pdf");
        assertThat(document.getMimeType()).isEqualTo(PDF_MIME);
        assertThat(document.getDeclaredDocType()).isEqualTo(DocumentType.LAB_RESULT);
        assertThat(document.getIdempotencyKey()).isEqualTo(KEY);
        assertThat(document.getRequestId()).matches("upload-\\d{8}-[0-9a-f]{8}");
        assertThat(analyses.findAll()).singleElement().satisfies(analysis -> {
            assertThat(analysis.getDocumentId()).isEqualTo(document.getId());
            assertThat(analysis.getStatus()).isEqualTo(DocumentAnalysisStatus.PENDING);
        });
        assertThat(storage.stored).containsExactly(document.getStorageKey());
        assertThat(publishedEvents).singleElement()
                .isInstanceOfSatisfying(DocumentUploadedEvent.class,
                        event -> assertThat(event.documentId()).isEqualTo(document.getId()));
    }

    @Test
    void activeCaregiverUploadsAsUploader() {
        DocumentUploadResult result = uploads.upload(caregiver, command(visitId, KEY));

        assertThat(result.created()).isTrue();
        assertThat(result.document().getUploaderUserId()).isEqualTo(caregiver.getId());
        assertThat(result.document().getPatientId()).isEqualTo(patientId);
    }

    @Test
    void unconnectedCaregiverIsForbiddenAndNothingIsSaved() {
        assertErrorCode(() -> uploads.upload(strangerCaregiver, command(visitId, KEY)),
                MedicalDocumentErrorCode.DOCUMENT_ACCESS_DENIED);

        assertNothingSaved();
    }

    @Test
    void missingVisitIsNotFound() {
        assertErrorCode(() -> uploads.upload(patient, command(UUID.randomUUID(), KEY)),
                MedicalDocumentErrorCode.VISIT_NOT_FOUND);
    }

    @Test
    void deletedVisitIsNotFound() {
        execute("UPDATE visits SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id", "id", visitId);

        assertErrorCode(() -> uploads.upload(patient, command(visitId, KEY)),
                MedicalDocumentErrorCode.VISIT_NOT_FOUND);
    }

    @Test
    void sameKeyReplayReturnsExistingResultWithoutSavingAgain() {
        DocumentUploadResult first = uploads.upload(patient, command(visitId, KEY));

        DocumentUploadResult again = uploads.upload(patient, command(visitId, KEY));

        assertThat(again.created()).isFalse();
        assertThat(again.document().getId()).isEqualTo(first.document().getId());
        assertThat(again.document().getRequestId()).isEqualTo(first.document().getRequestId());
        assertThat(again.latestAiJobStatus()).isEqualTo(AiJobStatus.QUEUED);
        assertThat(documents.count()).isEqualTo(1);
        assertThat(analyses.count()).isEqualTo(1);
        assertThat(storage.stored).hasSize(1);
        assertThat(publishedEvents).hasSize(1);
    }

    @Test
    void sameKeyForDifferentVisitIsConflict() {
        uploads.upload(patient, command(visitId, KEY));
        UUID otherVisitId = saveVisit();

        assertErrorCode(() -> uploads.upload(patient, command(otherVisitId, KEY)),
                MedicalDocumentErrorCode.IDEMPOTENCY_KEY_CONFLICT);
    }

    @Test
    void sameKeyWithDifferentFileIsConflict() {
        uploads.upload(patient, command(visitId, KEY));

        assertErrorCode(() -> uploads.upload(patient, command("사진.png", png())),
                MedicalDocumentErrorCode.IDEMPOTENCY_KEY_CONFLICT);

        assertThat(documents.count()).isEqualTo(1);
        assertThat(storage.stored).hasSize(1);
    }

    @Test
    void sameKeyWithNullHashReturnsExistingWithoutComparing() {
        MedicalDocument existing = saveDocumentWithoutHash();

        DocumentUploadResult result = uploads.upload(patient, command(visitId, KEY));

        assertThat(result.created()).isFalse();
        assertThat(result.document().getId()).isEqualTo(existing.getId());
        assertThat(storage.stored).isEmpty();
    }

    @Test
    void differentUsersWithSameKeyEachCreateDocument() {
        DocumentUploadResult byPatient = uploads.upload(patient, command(visitId, KEY));
        DocumentUploadResult byCaregiver = uploads.upload(caregiver, command(visitId, KEY));

        assertThat(byPatient.created()).isTrue();
        assertThat(byCaregiver.created()).isTrue();
        assertThat(documents.count()).isEqualTo(2);
    }

    @Test
    void sameKeyOfDeletedDocumentIsNotFound() {
        uploads.upload(patient, command(visitId, KEY)).document().delete(Instant.now());
        em.flush();

        assertErrorCode(() -> uploads.upload(patient, command(visitId, KEY)),
                MedicalDocumentErrorCode.DOCUMENT_NOT_FOUND);
    }

    @Test
    void missingBlankOrTooLongKeyIsRejected() {
        for (String key : new String[]{null, " ", "k".repeat(101)}) {
            assertErrorCode(() -> uploads.upload(patient, command(visitId, key)),
                    MedicalDocumentErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
    }

    @Test
    void keyOfHundredCharactersIsAccepted() {
        assertThat(uploads.upload(patient, command(visitId, "k".repeat(100))).created()).isTrue();
    }

    @Test
    void unknownDeclaredDocTypeIsRejected() {
        assertErrorCode(() -> uploads.upload(patient, command(visitId, KEY, "NOT_A_TYPE")),
                MedicalDocumentErrorCode.INVALID_DOC_TYPE);
    }

    @Test
    void missingDeclaredDocTypeIsSavedAsNull() {
        assertThat(uploads.upload(patient, command(visitId, KEY)).document().getDeclaredDocType()).isNull();
    }

    @Test
    void disguisedFileIsRejectedBeforeStoring() {
        DocumentUploadCommand disguised = command("검사결과.pdf", "plain text".getBytes());

        assertErrorCode(() -> uploads.upload(patient, disguised), MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);

        assertThat(storage.stored).isEmpty();
    }

    @Test
    void pdfContentWithPngFileNameIsStoredAsPdf() {
        MedicalDocument document = uploads.upload(patient, command("사진.png", PDF)).document();

        assertThat(document.getMimeType()).isEqualTo(PDF_MIME);
        assertThat(document.getStorageKey()).endsWith("/original.pdf");
    }

    @Test
    void storageFailureIsUnavailableAndNothingIsSaved() {
        storage.failOnStore = true;

        assertErrorCode(() -> uploads.upload(patient, command(visitId, KEY)),
                MedicalDocumentErrorCode.STORAGE_UNAVAILABLE);

        assertNothingSaved();
    }

    @Test
    void databaseFailureDeletesStoredOriginal() {
        DocumentUploadCommandService failing = uploadService(failingSaver());

        assertThatThrownBy(() -> failing.upload(patient, command(visitId, KEY)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(storage.stored).hasSize(1);
        assertThat(storage.deleted).containsExactlyElementsOf(storage.stored);
    }

    @Test
    void failedCompensationKeepsOriginalException() {
        storage.failOnDelete = true;
        DocumentUploadCommandService failing = uploadService(failingSaver());

        assertThatThrownBy(() -> failing.upload(patient, command(visitId, KEY)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void savingSameUploaderAndKeyTwiceViolatesUniqueConstraint() {
        MedicalDocument first = uploads.upload(patient, command(visitId, KEY)).document();

        assertThatThrownBy(() -> newSaver().registerUpload(duplicateOf(first)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void pngUploadKeepsPngExtensionAndMimeType() {
        MedicalDocument document = uploads.upload(patient, command("사진.png", png())).document();

        assertThat(document.getStorageKey()).endsWith("/original.png");
        assertThat(document.getMimeType()).isEqualTo("image/png");
    }

    @Test
    void concurrentSameKeyReturnsWinnerAndDeletesOwnOriginal() {
        DocumentUploadCommandService racing = uploadService(conflictingSaver(true));

        DocumentUploadResult result = racing.upload(patient, command(visitId, KEY));

        assertThat(result.created()).isFalse();
        assertThat(result.document().getId()).isEqualTo(winner.getId());
        assertThat(storage.stored).hasSize(1);
        assertThat(storage.deleted).containsExactlyElementsOf(storage.stored);
    }

    @Test
    void failedCompensationOnConcurrentSameKeyStillReturnsWinner() {
        storage.failOnDelete = true;
        DocumentUploadCommandService racing = uploadService(conflictingSaver(true));

        DocumentUploadResult result = racing.upload(patient, command(visitId, KEY));

        assertThat(result.created()).isFalse();
        assertThat(result.document().getId()).isEqualTo(winner.getId());
    }

    @Test
    void integrityViolationWithoutSameKeyDocumentIsRethrown() {
        DocumentUploadCommandService racing = uploadService(conflictingSaver(false));

        assertThatThrownBy(() -> racing.upload(patient, command(visitId, KEY)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(storage.stored).hasSize(1);
        assertThat(storage.deleted).containsExactlyElementsOf(storage.stored);
    }

    @Test
    void pathInFileNameIsStripped() {
        assertThat(uploadedFileName("key-windows", "C:\\a\\b.pdf")).isEqualTo("b.pdf");
        assertThat(uploadedFileName("key-relative", "../x.pdf")).isEqualTo("x.pdf");
    }

    @Test
    void missingFileNameFallsBackToOriginalWithExtension() {
        assertThat(uploadedFileName("key-null", null)).isEqualTo("original.pdf");
        assertThat(uploadedFileName("key-empty", "")).isEqualTo("original.pdf");
    }

    @Test
    void longFileNameIsCutToMaxLength() {
        assertThat(uploadedFileName("key-long", "a".repeat(300))).isEqualTo("a".repeat(255));
    }

    @Test
    void cutDoesNotLeaveHalfOfSurrogatePair() {
        String name = "a".repeat(254) + "😀" + ".pdf";

        assertThat(uploadedFileName("key-emoji", name)).isEqualTo("a".repeat(254));
    }

    private DocumentUploadSaveCommandService newSaver() {
        return new DocumentUploadSaveCommandService(documents, analyses, publishedEvents::add, "test-parser");
    }

    /** DB 저장이 실패한 것처럼 예외를 던지는 saver를 만듭니다. */
    private DocumentUploadSaveCommandService failingSaver() {
        return new DocumentUploadSaveCommandService(documents, analyses, publishedEvents::add, "test-parser") {
            @Override
            public MedicalDocument registerUpload(MedicalDocument document) {
                throw new IllegalStateException("DB 저장 실패");
            }
        };
    }

    private DocumentUploadCommandService uploadService(DocumentUploadSaveCommandService saver) {
        return new DocumentUploadCommandService(
                documents,
                new MedicalDocumentAccessPolicy(relations, patients),
                new DocumentFileValidator(20_971_520L, 30, 40_000_000L),
                storage,
                saver);
    }

    /** unique 제약 위반을 던지는 saver를 만듭니다. saveWinner가 true면 던지기 전에 같은 업로더·같은 키 문서를 먼저 저장합니다. */
    private DocumentUploadSaveCommandService conflictingSaver(boolean saveWinner) {
        return new DocumentUploadSaveCommandService(documents, analyses, publishedEvents::add, "test-parser") {
            @Override
            public MedicalDocument registerUpload(MedicalDocument document) {
                if (saveWinner) {
                    winner = saveWinnerOf(document);
                }
                throw new DataIntegrityViolationException("documents_uploader_idempotency_key_uidx");
            }
        };
    }

    /** 동시에 들어온 요청이 먼저 저장한 것처럼, 같은 업로더·방문·키를 가진 다른 문서를 저장해 반환합니다. */
    private MedicalDocument saveWinnerOf(MedicalDocument loser) {
        return documents.saveAndFlush(duplicateOf(loser));
    }

    /** 주어진 문서와 업로더·방문·키가 같고 id만 다른 문서를 저장하지 않은 채로 만들어 반환합니다. */
    private static MedicalDocument duplicateOf(MedicalDocument source) {
        return MedicalDocument.uploadedWithId(
                UUID.randomUUID(), source.getPatientId(), source.getVisitId(), source.getUploaderUserId(),
                "먼저.pdf", "documents/duplicate/original.pdf", PDF_MIME, 1L,
                "upload-duplicate", source.getIdempotencyKey(), null, null);
    }

    /** 해시 컬럼이 생기기 전에 올린 것처럼, 기본 방문·키에 contentSha256이 null인 문서를 저장해 반환합니다. */
    private MedicalDocument saveDocumentWithoutHash() {
        return documents.saveAndFlush(MedicalDocument.uploadedWithId(
                UUID.randomUUID(), patientId, visitId, patient.getId(),
                "예전.pdf", "documents/legacy/original.pdf", PDF_MIME, 1L,
                "upload-legacy", KEY, null, null));
    }

    private DocumentUploadCommand command(UUID visitId, String key) {
        return command(visitId, key, null);
    }

    private DocumentUploadCommand command(UUID visitId, String key, String declaredDocType) {
        return new DocumentUploadCommand(visitId, key, declaredDocType, "검사결과.pdf", PDF);
    }

    /** 기본 방문·키에 파일명과 내용만 바꾼 업로드 입력을 만듭니다. */
    private DocumentUploadCommand command(String fileName, byte[] content) {
        return new DocumentUploadCommand(visitId, KEY, null, fileName, content);
    }

    private String uploadedFileName(String key, String fileName) {
        DocumentUploadCommand command = new DocumentUploadCommand(visitId, key, null, fileName, PDF);
        return uploads.upload(patient, command).document().getFileName();
    }

    private User createUser(String kakaoId, String name, UserRole role) {
        return accounts.chooseRole(accounts.loginOrRegister(kakaoId, name).getKakaoId(), role);
    }

    private void connect(User caregiver, User patient) {
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        CareRelation requested = cares.request(caregiver, accounts.patientInviteCode(patient));
        cares.accept(patient, requested.getId());
    }

    /** 환자의 삭제되지 않은 방문 행을 넣고 그 id를 반환합니다. */
    private UUID saveVisit() {
        UUID id = UUID.randomUUID();
        execute("INSERT INTO visits (id, patient_id, visited_on) VALUES (:id, :patientId, :visitedOn)",
                "id", id, "patientId", patientId, "visitedOn", LocalDate.of(2026, 9, 20));
        return id;
    }

    private void assertNothingSaved() {
        assertThat(storage.stored).isEmpty();
        assertThat(documents.count()).isZero();
        assertThat(analyses.count()).isZero();
    }

    /** 테스트 SQL을 named parameter와 함께 실행합니다. */
    private void execute(String sql, Object... parameters) {
        var query = em.getEntityManager().createNativeQuery(sql);
        for (int i = 0; i < parameters.length; i += 2) {
            query.setParameter(String.valueOf(parameters[i]), parameters[i + 1]);
        }
        query.executeUpdate();
    }

    private static void assertErrorCode(ThrowingCallable call, MedicalDocumentErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private static byte[] pdf() {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] png() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return output.toByteArray();
    }

    /** 저장·삭제 호출을 기록하고, failOnStore·failOnDelete가 켜지면 저장·삭제를 실패시키는 테스트용 저장소입니다. */
    private static class RecordingStorage implements DocumentStoragePort {

        private final List<String> stored = new ArrayList<>();
        private final List<String> deleted = new ArrayList<>();
        private boolean failOnStore;
        private boolean failOnDelete;

        @Override
        public String createSignedUrl(String storageKey, Duration ttl) {
            return "https://storage.test/" + storageKey;
        }

        @Override
        public void storeOriginal(String storageKey, byte[] content, String mimeType) {
            if (failOnStore) {
                throw new DocumentStorageException("저장 실패");
            }
            stored.add(storageKey);
        }

        @Override
        public void deleteOriginal(String storageKey) {
            if (failOnDelete) {
                throw new DocumentStorageException("삭제 실패");
            }
            deleted.add(storageKey);
        }
    }
}
