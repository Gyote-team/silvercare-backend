package com.gyote.silvercare.medical_document.api.dto.response;

public record AiDocumentSectionItemResponseDto(
        String sentenceId,
        String sourceItemId,
        String label,
        String value,
        String unit,
        boolean hasSource,
        String citationId
) {
}
