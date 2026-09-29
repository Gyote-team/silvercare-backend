package com.gyote.silvercare.medical_document.api.dto.response;

import java.util.List;

public record AiDocumentSectionsResponseDto(
        String documentId,
        List<AiDocumentSectionResponseDto> sections
) {
}
