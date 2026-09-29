package com.gyote.silvercare.medical_document.api.dto.response;

import java.util.List;

public record AiDocumentSectionResponseDto(
        String sectionId,
        String sectionType,
        String title,
        List<AiDocumentSectionItemResponseDto> items
) {
}
