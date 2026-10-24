package com.gyote.silvercare.medical_document.api.dto.response;
import java.time.Instant;

/** 특정 문서 페이지를 열기 위한 서명 URL 응답입니다. */
public record AiDocumentPageResponseDto(
        String documentId,
        int pageNo,
        String pageUrl,
        Instant expiresAt,
        boolean renderedPage,
        String anchorId,
        AiDocumentCitationsResponseDto.SourceBox sourceBox,
        Integer pageWidthPx,
        Integer pageHeightPx
) {
}
