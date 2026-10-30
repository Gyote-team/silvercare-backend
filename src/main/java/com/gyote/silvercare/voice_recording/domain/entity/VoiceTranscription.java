package com.gyote.silvercare.voice_recording.domain.entity;

import com.gyote.silvercare.global.status.TranscriptionStatus;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** 원본과 STT 초안 및 사용자 확정 결과를 분리해 보존한다. */
@Entity
@Table(
        name = "voice_transcriptions",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"uploader_user_id", "idempotency_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoiceTranscription {
    @Id private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "uploader_user_id", nullable = false)
    private UUID uploaderUserId;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(nullable = false, length = 20)
    private String purpose;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "storage_key", nullable = false, length = 300)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "consent_at", nullable = false)
    private Instant consentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TranscriptionStatus status;

    @Column(name = "draft_text", columnDefinition = "TEXT")
    private String draftText;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "record_id")
    private UUID recordId;

    @Column(name = "confirmed_text", columnDefinition = "TEXT")
    private String confirmedText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "lease_token")
    private UUID leaseToken;

    @Version private long version;

    /** 접근 검증을 마친 음성을 아직 확정되지 않은 작업으로 등록한다. */
    public static VoiceTranscription create(
            UUID id,
            UUID patient,
            UUID uploader,
            UUID visit,
            String purpose,
            String key,
            String hash,
            String storageKey,
            String mime,
            long size) {
        VoiceTranscription row = new VoiceTranscription();
        row.id = id;
        row.patientId = patient;
        row.uploaderUserId = uploader;
        row.visitId = visit;
        row.purpose = purpose;
        row.idempotencyKey = key;
        row.contentHash = hash;
        row.storageKey = storageKey;
        row.contentType = mime;
        row.sizeBytes = size;
        row.createdAt = row.updatedAt = row.consentAt = Instant.now();
        row.status = TranscriptionStatus.UPLOADED;
        return row;
    }

    /** 한 번 생성한 확정 기록의 ID를 보존한다. */
    public void confirm(UUID record, String body) {
        recordId = record;
        confirmedText = body;
        status = TranscriptionStatus.CONFIRMED;
        updatedAt = Instant.now();
    }

    /** 확정 전 작업을 취소하고 이후 도착하는 AI 결과를 무효화한다. */
    public void cancel() {
        status = TranscriptionStatus.CANCELED;
        draftText = null;
        errorCode = null;
        leaseToken = null;
        updatedAt = Instant.now();
    }

    /** 실패한 작업만 명시적인 사용자 요청으로 다시 대기시킨다. */
    public void retry() {
        status = TranscriptionStatus.UPLOADED;
        draftText = null;
        errorCode = null;
        attempts = 0;
        leaseToken = null;
        updatedAt = Instant.now();
    }
}
