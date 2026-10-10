package com.gyote.silvercare.health_record.api.dto.response;
import java.util.List;
public record HealthRecordPageResponseDto(List<HealthRecordResponseDto> items, String nextCursor, boolean hasNext) {}
