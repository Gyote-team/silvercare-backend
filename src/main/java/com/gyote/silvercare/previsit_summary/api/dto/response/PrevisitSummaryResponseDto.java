package com.gyote.silvercare.previsit_summary.api.dto.response;

import java.time.Instant;
import java.util.*;

/** 요청·작업 상태·결과의 공개 HTTP 응답 계약. */
public final class PrevisitSummaryResponseDto {
    private PrevisitSummaryResponseDto() {}
    public record Accepted(UUID requestId,String statusUrl) {}
    public record Operation(UUID requestId,String operationType,String jobStatus,String resultStatus,
                            UUID summaryId,boolean retryable,String errorCode) {}
    public record Source(UUID healthRecordId,Instant recordedAt,String content) {}
    public record Detail(UUID summaryId,String summary,List<UUID> usedHealthRecordIds,int windowDays,
                         Instant createdAt,List<Source> sourceRecords) {}
}
