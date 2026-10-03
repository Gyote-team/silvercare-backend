package com.gyote.silvercare.medical_document.domain.entity;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.global.type.DocumentType;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 의료 문서 메타데이터입니다. 원본 파일은 Object Storage에 두고 storage key만 저장합니다.
 * 테이블은 V2의 documents와 add_document_patient_and_status_fields migration으로 만들며, 컬럼은 migration과 일치해야 합니다.
 */
@Entity
@Table(
        name = "documents",
        uniqueConstraints = @UniqueConstraint(
                name = "documents_uploader_idempotency_key_uidx",
                columnNames = {"uploader_user_id", "idempotency_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalDocument {

    @Id
    @Column(length = 36, nullable = false)
    private UUID id;

    // patients.id
    @Column(name = "patient_id", nullable = false, length = 36)
    private UUID patientId;

    @Column(name = "visit_id", nullable = false, length = 36)
    private UUID visitId;

    @Column(name = "uploader_user_id", nullable = false, length = 36)
    private UUID uploaderUserId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    // 업로드할 때 사용자가 고른 유형이며, 분석으로 판별한 documentType과는 별개입니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "declared_doc_type", length = 30)
    private DocumentType declaredDocType;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** 업로드 직후 문서를 만듭니다. 상태는 UPLOADED, 유형은 분류 전이므로 UNKNOWN입니다. */
    public static MedicalDocument uploaded(
            UUID patientId,
            UUID visitId,
            UUID uploaderUserId,
            String fileName,
            String storageKey,
            String mimeType,
            long fileSizeBytes,
            String requestId
    ) {
        return uploadedWithId(null, patientId, visitId, uploaderUserId, fileName, storageKey,
                mimeType, fileSizeBytes, requestId, null, null);
    }

    /** id를 미리 정해서 업로드 직후 문서를 만듭니다. storage key에 문서 id를 넣어야 할 때 사용합니다. */
    public static MedicalDocument uploadedWithId(
            UUID id,
            UUID patientId,
            UUID visitId,
            UUID uploaderUserId,
            String fileName,
            String storageKey,
            String mimeType,
            long fileSizeBytes,
            String requestId,
            String idempotencyKey,
            DocumentType declaredDocType
    ) {
        MedicalDocument document = new MedicalDocument();
        document.id = id;
        document.assignOwner(patientId, visitId, uploaderUserId);
        document.assignFile(fileName, storageKey, mimeType, fileSizeBytes);
        document.assignUploadRequest(requestId, idempotencyKey, declaredDocType);
        document.documentType = DocumentType.UNKNOWN;
        document.status = DocumentStatus.UPLOADED;
        return document;
    }

    private void assignOwner(UUID patientId, UUID visitId, UUID uploaderUserId) {
        this.patientId = patientId;
        this.visitId = visitId;
        this.uploaderUserId = uploaderUserId;
    }

    private void assignFile(String fileName, String storageKey, String mimeType, long fileSizeBytes) {
        this.fileName = fileName;
        this.storageKey = storageKey;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
    }

    private void assignUploadRequest(String requestId, String idempotencyKey, DocumentType declaredDocType) {
        this.requestId = requestId;
        this.idempotencyKey = idempotencyKey;
        this.declaredDocType = declaredDocType;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (statusChangedAt == null) {
            statusChangedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** 소프트 삭제합니다. 이미 삭제된 문서는 없는 문서로 취급합니다. */
    public void delete(Instant now) {
        if (isDeleted()) {
            throw new BusinessException(MedicalDocumentErrorCode.DOCUMENT_NOT_FOUND);
        }
        status = DocumentStatus.DELETED;
        deletedAt = now;
        statusChangedAt = now;
    }

    /** 소프트 삭제된 문서인지 반환합니다. */
    public boolean isDeleted() {
        return status == DocumentStatus.DELETED;
    }
}