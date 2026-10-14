package com.gyote.silvercare.ai_result.api.dto;

import com.gyote.silvercare.ai_result.domain.QuestionType;
import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.status.ResultStatus;

import java.time.Instant;
import java.util.UUID;

public record AiResultDocumentResponse(
        UUID resultDocumentId,
        UUID requestId,
        UUID patientId,
        QuestionType questionType,
        AiJobStatus jobStatus,
        ResultStatus resultStatus,
        String title,
        String content,
        String citationJson,
        String errorMessage,
        Instant createdAt,
        Instant completedAt,
        String statusUrl
) {
}
