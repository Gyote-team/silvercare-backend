package com.gyote.silvercare.medical_document.query.model;

import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.global.status.ResultStatus;
import com.gyote.silvercare.global.type.DocumentType;

import java.time.Instant;
import java.util.UUID;

public record AiDocumentDetailView(
        UUID documentId,
        String documentName,
        DocumentType documentType,
        UUID visitId,
        DocumentStatus documentStatus,
        AiJobStatus jobStatus,
        ResultStatus resultStatus,
        String title,
        String content,
        Instant createdAt,
        long sectionCount,
        long citationCount
) {
}
