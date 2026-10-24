package com.gyote.silvercare.health_record.api.dto.response;

import java.util.List;

/** 건강기록 목록과 다음 조회 커서를 반환하는 HTTP 응답. */
public record HealthRecordPageResponseDto(
        List<HealthRecordResponseDto> items, String nextCursor, boolean hasNext) {}
