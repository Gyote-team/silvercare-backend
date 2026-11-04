package com.gyote.silvercare.previsit_summary.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/** 요청과 저장 결과의 식별자를 분리한 진료 전 요약 작업. */
@Entity
@Table(name = "previsit_summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrevisitSummary {
    @Id private UUID id;
    @Column(nullable = false) private UUID patientId;
    @Column(nullable = false) private UUID requestedByUserId;
    @Column(nullable = false, length = 100) private String idempotencyKey;
    @Column(nullable = false) private int windowDays;
    @Column(nullable = false) private Instant windowStart;
    @Column(nullable = false) private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private UUID executionId;
    private UUID summaryId;
    @Column(nullable = false, length = 20) private String jobStatus;
    @Column(length = 20) private String resultStatus;
    @Column(columnDefinition = "TEXT") private String summary;
    @Column(columnDefinition = "TEXT") private String usedRecordIds;
    @Column(length = 60) private String errorCode;
    @Column(nullable = false) private boolean retryable;
    @Version private long version;

    /** 서버가 선정한 기간과 요청자를 고정해 대기 작업을 생성한다. */
    public static PrevisitSummary create(UUID patient, UUID actor, String key, int days, Instant now) {
        var row = new PrevisitSummary();
        row.id = UUID.randomUUID(); row.patientId = patient; row.requestedByUserId = actor;
        row.idempotencyKey = key; row.windowDays = days; row.createdAt = now;
        row.windowStart = now.minus(java.time.Duration.ofDays(days)); row.jobStatus = "QUEUED";
        return row;
    }

    /** 외부 호출 전에 실행 토큰을 발급한다. */
    public void start() {
        jobStatus = "RUNNING"; startedAt = Instant.now(); executionId = UUID.randomUUID();
    }

    /** 검증된 결과를 저장하고 조회용 결과 식별자를 생성한다. */
    public void succeed(String text, String ids, String completeness) {
        summary = text; usedRecordIds = ids; resultStatus = completeness;
        summaryId = UUID.randomUUID(); jobStatus = "SUCCEEDED"; completedAt = Instant.now();
    }

    /** 오류 본문 대신 안전한 실패 코드만 남긴다. 자동 재전송은 하지 않는다. */
    public void fail(String code, boolean canRetry) {
        jobStatus = "FAILED"; errorCode = code; retryable = canRetry;
        completedAt = Instant.now(); summary = null; usedRecordIds = null;
        summaryId = null; resultStatus = null;
    }
}
