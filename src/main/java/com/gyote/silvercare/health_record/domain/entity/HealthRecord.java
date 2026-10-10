package com.gyote.silvercare.health_record.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** 개인 프로필에 귀속되며 작성자를 별도로 보존하는 건강기록. */
@Entity
@Table(name = "health_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthRecord {
    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "author_user_id", nullable = false)
    private UUID authorUserId;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "input_type", nullable = false, length = 10)
    private String inputType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "proxy_written", nullable = false)
    private boolean proxyWritten;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version private long version;

    /** 텍스트 건강기록을 생성하고 서버 시각을 기록한다. */
    public static HealthRecord create(
            UUID patientId, UUID authorId, UUID visitId, String body, boolean proxy) {
        HealthRecord record = new HealthRecord();
        record.id = UUID.randomUUID();
        record.patientId = patientId;
        record.authorUserId = authorId;
        record.visitId = visitId;
        record.content = body;
        record.inputType = "TEXT";
        record.proxyWritten = proxy;
        record.recordedAt = record.createdAt = record.updatedAt = Instant.now();
        return record;
    }

    /** 본문과 연결 방문을 수정한다. */
    public void update(String body, UUID visitId) {
        this.content = body;
        this.visitId = visitId;
        this.updatedAt = Instant.now();
    }

    /** 참조를 보존하면서 일반 조회에서 제외한다. */
    public void delete() {
        this.deletedAt = this.updatedAt = Instant.now();
    }
}
