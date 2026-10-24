package com.gyote.silvercare.ai_result.api.dto.response;

import java.util.UUID;

/** 원문 근거 표시에 필요한 AI 답변 문서 응답이다. */
public record AiResultDocumentResponseDto(
        UUID resultDocumentId,
        String citationJson,
        String errorMessage
) {
}
