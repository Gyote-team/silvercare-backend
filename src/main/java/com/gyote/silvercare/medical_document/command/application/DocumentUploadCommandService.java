package com.gyote.silvercare.medical_document.command.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.type.DocumentType;
import com.gyote.silvercare.medical_document.domain.DocumentFileType;
import com.gyote.silvercare.medical_document.domain.DocumentFileValidator;
import com.gyote.silvercare.medical_document.domain.DocumentStorageException;
import com.gyote.silvercare.medical_document.domain.DocumentStoragePort;
import com.gyote.silvercare.medical_document.domain.MedicalDocumentAccessPolicy;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import com.gyote.silvercare.medical_document.query.model.MedicalDocumentAiStatusRow;
import com.gyote.silvercare.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 문서 업로드 전체 흐름(입력 확인·권한·멱등 처리·원본 저장·DB 저장)을 조율하는 코드입니다.
 * 원본 저장이 DB 트랜잭션 밖에 있도록 이 클래스에는 @Transactional을 걸지 않습니다.
 */
@Service
public class DocumentUploadCommandService {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadCommandService.class);
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final MedicalDocumentRepository documents;
    private final MedicalDocumentAccessPolicy accessPolicy;
    private final DocumentFileValidator validator;
    private final DocumentStoragePort storage;
    private final DocumentUploadSaveCommandService saver;

    public DocumentUploadCommandService(
            MedicalDocumentRepository documents,
            MedicalDocumentAccessPolicy accessPolicy,
            DocumentFileValidator validator,
            DocumentStoragePort storage,
            DocumentUploadSaveCommandService saver
    ) {
        this.documents = documents;
        this.accessPolicy = accessPolicy;
        this.validator = validator;
        this.storage = storage;
        this.saver = saver;
    }

    /** 문서를 업로드하고 결과를 반환합니다. 같은 Idempotency-Key 재요청이면 저장 없이 기존 결과(created=false)를 반환합니다. */
    public DocumentUploadResult upload(User me, DocumentUploadCommand command) {
        checkIdempotencyKey(command.idempotencyKey());
        DocumentType declaredDocType = toDeclaredDocType(command.declaredDocType());
        UUID patientId = requireUploadablePatientId(me, command.visitId());
        Optional<DocumentUploadResult> replay = findReplay(me, command);
        if (replay.isPresent()) {
            return replay.get();
        }
        DocumentFileType fileType = validator.validate(command.content());
        MedicalDocument document = newDocument(me, patientId, command, declaredDocType, fileType);
        storeOriginal(document, command.content());
        return saveOrReplay(me, command, document);
    }

    private static void checkIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessException(MedicalDocumentErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
    }

    private static DocumentType toDeclaredDocType(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return DocumentType.valueOf(rawValue);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(MedicalDocumentErrorCode.INVALID_DOC_TYPE);
        }
    }

    private UUID requireUploadablePatientId(User me, UUID visitId) {
        UUID patientId = documents.findPatientIdByVisitId(visitId)
                .map(UUID::fromString)
                .orElseThrow(() -> new BusinessException(MedicalDocumentErrorCode.VISIT_NOT_FOUND));
        if (!accessPolicy.canUpload(me, patientId)) {
            throw new BusinessException(MedicalDocumentErrorCode.DOCUMENT_ACCESS_DENIED);
        }
        return patientId;
    }

    private Optional<DocumentUploadResult> findReplay(User me, DocumentUploadCommand command) {
        return documents.findByUploaderUserIdAndIdempotencyKey(me.getId(), command.idempotencyKey())
                .map(existing -> toReplayResult(existing, command.visitId()));
    }

    private DocumentUploadResult toReplayResult(MedicalDocument existing, UUID visitId) {
        if (!existing.getVisitId().equals(visitId)) {
            throw new BusinessException(MedicalDocumentErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        if (existing.isDeleted()) {
            throw new BusinessException(MedicalDocumentErrorCode.DOCUMENT_NOT_FOUND);
        }
        return new DocumentUploadResult(existing, findLatestAiJobStatus(existing.getId()), false);
    }

    private AiJobStatus findLatestAiJobStatus(UUID documentId) {
        return documents.findAiStatusesByDocumentIds(List.of(documentId)).stream()
                .map(MedicalDocumentAiStatusRow::getJobStatus)
                .filter(Objects::nonNull)
                .findFirst()
                .map(AiJobStatus::valueOf)
                .orElse(null);
    }

    private static MedicalDocument newDocument(
            User me,
            UUID patientId,
            DocumentUploadCommand command,
            DocumentType declaredDocType,
            DocumentFileType fileType
    ) {
        UUID documentId = UUID.randomUUID();
        String storageKey = "documents/" + documentId + "/original." + fileType.getExtension();
        return MedicalDocument.uploadedWithId(
                documentId, patientId, command.visitId(), me.getId(),
                toFileName(command.fileName(), fileType), storageKey, fileType.getMimeType(),
                command.content().length, newRequestId(), command.idempotencyKey(), declaredDocType);
    }

    private static String toFileName(String original, DocumentFileType fileType) {
        String name = original == null ? "" : original.trim();
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        if (name.isBlank()) {
            return "original." + fileType.getExtension();
        }
        return truncate(name);
    }

    private static String truncate(String name) {
        if (name.length() <= MAX_FILE_NAME_LENGTH) {
            return name;
        }
        // 잘리는 자리가 서로게이트 쌍의 앞쪽 절반이면 반쪽 문자가 남지 않게 그 문자도 버립니다.
        boolean splitsPair = Character.isHighSurrogate(name.charAt(MAX_FILE_NAME_LENGTH - 1));
        return name.substring(0, splitsPair ? MAX_FILE_NAME_LENGTH - 1 : MAX_FILE_NAME_LENGTH);
    }

    private static String newRequestId() {
        String date = LocalDate.now(SEOUL).format(DateTimeFormatter.BASIC_ISO_DATE);
        return "upload-" + date + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void storeOriginal(MedicalDocument document, byte[] content) {
        try {
            storage.storeOriginal(document.getStorageKey(), content, document.getMimeType());
        } catch (DocumentStorageException e) {
            throw new BusinessException(MedicalDocumentErrorCode.STORAGE_UNAVAILABLE);
        }
    }

    private DocumentUploadResult saveOrReplay(User me, DocumentUploadCommand command, MedicalDocument document) {
        try {
            return new DocumentUploadResult(saver.registerUpload(document), AiJobStatus.QUEUED, true);
        } catch (DataIntegrityViolationException e) {
            // 같은 키로 동시에 들어온 요청이 먼저 저장된 경우입니다. 방금 올린 원본을 지우고 먼저 저장된 결과를 돌려줍니다.
            deleteOriginal(document.getStorageKey());
            return findReplay(me, command).orElseThrow(() -> e);
        } catch (RuntimeException e) {
            deleteOriginal(document.getStorageKey());
            throw e;
        }
    }

    private void deleteOriginal(String storageKey) {
        try {
            storage.deleteOriginal(storageKey);
        } catch (DocumentStorageException e) {
            // 보상 삭제 실패가 원래 예외를 가리지 않도록 기록만 남깁니다.
            log.warn("업로드 실패 후 원본을 지우지 못했습니다. storageKey={}", storageKey, e);
        }
    }
}
