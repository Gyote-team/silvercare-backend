package com.gyote.silvercare.visit.api.dto.response;

import java.util.List;

/** 방문 목록과 불투명 커서의 HTTP 응답. */
public record VisitPageResponseDto(
        List<VisitResponseDto> items, String nextCursor, boolean hasNext) {}
