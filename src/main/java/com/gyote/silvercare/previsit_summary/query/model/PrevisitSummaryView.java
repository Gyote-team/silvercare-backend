package com.gyote.silvercare.previsit_summary.query.model;

import java.time.Instant;
import java.util.*;

/** HTTP DTO와 분리한 요청 상태와 검증된 요약 결과 조회 모델. */
public final class PrevisitSummaryView {
    private PrevisitSummaryView() {}
    public record Operation(UUID requestId, String operationType, String jobStatus,
            String resultStatus, UUID summaryId, boolean retryable, String errorCode) {}
    public record Source(UUID healthRecordId, Instant recordedAt, String content) {}
    public record Detail(UUID summaryId, String summary, List<UUID> usedHealthRecordIds,
            int windowDays, Instant createdAt, List<Source> sourceRecords) {}
}
