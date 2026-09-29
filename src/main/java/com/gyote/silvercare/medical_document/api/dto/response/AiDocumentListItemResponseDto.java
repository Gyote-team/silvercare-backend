package com.gyote.silvercare.medical_document.api.dto.response;

import java.time.Instant;

public record AiDocumentListItemResponseDto(
        String documentId,
        String documentName,
        String documentType,
        String visitId,
        String visitedOn,
        Instant createdAt,
        String authorName,
        String documentStatus,
        String jobStatus,
        String resultStatus
) {
}
