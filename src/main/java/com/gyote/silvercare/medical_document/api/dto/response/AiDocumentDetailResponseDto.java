package com.gyote.silvercare.medical_document.api.dto.response;

import java.time.Instant;

public record AiDocumentDetailResponseDto(
        String documentId,
        String documentName,
        String documentType,
        String visitId,
        String documentStatus,
        String jobStatus,
        String resultStatus,
        String title,
        String content,
        Instant createdAt,
        long sectionCount,
        long citationCount
) {
}
