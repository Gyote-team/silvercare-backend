package com.gyote.silvercare.medical_document.query.model;

import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.global.status.ResultStatus;
import com.gyote.silvercare.global.type.DocumentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AiDocumentListView(
        UUID documentId,
        String documentName,
        DocumentType documentType,
        UUID visitId,
        LocalDate visitedOn,
        Instant createdAt,
        String authorName,
        DocumentStatus documentStatus,
        AiJobStatus jobStatus,
        ResultStatus resultStatus
) {
}
