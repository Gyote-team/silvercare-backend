package com.gyote.silvercare.medical_document.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.global.status.ResultStatus;
import com.gyote.silvercare.global.type.DocumentType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MedicalDocumentResponse(
        UUID documentId,
        UUID visitId,
        UUID patientId,
        String documentName,
        DocumentType documentType,
        LocalDate visitedOn,
        Author author,
        DocumentStatus documentStatus,
        AiJobStatus latestAiJobStatus,
        ResultStatus resultStatus,
        OffsetDateTime statusChangedAt,
        boolean retryable,
        @JsonInclude(JsonInclude.Include.NON_NULL) String signedUrl,
        OffsetDateTime createdAt
) {

    public record Author(String name, String role) {
    }
}
