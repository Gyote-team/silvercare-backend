package com.gyote.silvercare.schedule.api.dto.response;

import java.util.List;

public record SchedulePageResponseDto(
        List<ScheduleResponseDto> items, int page, int size, long totalCount) {}
