package com.gyote.silvercare.medical_document.api.dto.response;

import java.time.Instant;

public record AiDocumentExplanationStatusResponseDto(
        String documentId,
        String jobStatus,
        String resultStatus,
        String currentStep,
        Integer progress,
        Instant completedAt,
        String detailUrl,
        String failedStep,
        String errorCode,
        boolean retryable,
        String originalDocumentUrl
) {
}
