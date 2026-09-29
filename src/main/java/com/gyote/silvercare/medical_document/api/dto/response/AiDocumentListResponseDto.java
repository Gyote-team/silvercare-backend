package com.gyote.silvercare.medical_document.api.dto.response;

import java.util.List;

public record AiDocumentListResponseDto(
        List<AiDocumentListItemResponseDto> items,
        int page,
        int size,
        long totalCount
) {
}
