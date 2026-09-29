package com.gyote.silvercare.medical_document.api.dto.request;

public record AiDocumentListRequestDto(
        String patientId,
        String visitId,
        String docType,
        String status,
        int page,
        int size
) {
}
