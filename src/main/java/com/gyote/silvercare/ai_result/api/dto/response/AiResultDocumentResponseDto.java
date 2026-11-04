package com.gyote.silvercare.ai_result.api.dto.response;

import java.util.UUID;
import java.time.Instant;

/** 원문 근거 표시에 필요한 AI 답변 문서 응답이다. */
public record AiResultDocumentResponseDto(
        UUID resultDocumentId,
        String jobStatus,
        String resultStatus,
        String title,
        String content,
        String contentFormat,
        String questionType,
        String saveStatus,
        Instant savedAt,
        String citationJson,
        String errorMessage
) {
}
