package com.gyote.silvercare.health_record.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** 개인별 건강 상태 기록입니다. patientId가 모든 조회·권한 검증의 기준이 됩니다. */
@Entity
@Table(name = "health_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthRecord {

    @Id
    private UUID id;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "author_user_id", nullable = false)
    private UUID authorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 10)
    private HealthRecordInputType inputType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "proxy_written", nullable = false)
    private boolean proxyWritten;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static HealthRecord create(
            UUID patientId,
            UUID authorUserId,
            UUID visitId,
            HealthRecordInputType inputType,
            String content,
            Instant recordedAt,
            boolean proxyWritten,
            String idempotencyKey
    ) {
        HealthRecord record = new HealthRecord();
        record.patientId = patientId;
        record.authorUserId = authorUserId;
        record.visitId = visitId;
        record.inputType = inputType;
        record.content = content;
        record.recordedAt = recordedAt;
        record.proxyWritten = proxyWritten;
        record.idempotencyKey = idempotencyKey;
        return record;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
