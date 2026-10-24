package com.gyote.silvercare.medical_document.query.model;

import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.status.ResultStatus;

import java.time.Instant;
import java.util.UUID;

public record AiDocumentExplanationStatusView(
        UUID documentId,
        AiJobStatus jobStatus,
        ResultStatus resultStatus,
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
