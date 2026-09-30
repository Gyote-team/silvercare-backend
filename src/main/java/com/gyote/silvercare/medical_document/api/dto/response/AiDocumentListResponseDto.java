package com.gyote.silvercare.medical_document.api.dto.response;

import java.util.List;

public record AiDocumentListResponseDto(
        // 현재 페이지의 의료 문서 목록입니다. 예: [{"documentId":"doc-001", ...}]
        List<AiDocumentListItemResponseDto> items,
        // 현재 페이지 번호입니다. 예: 0
        int page,
        // 페이지당 문서 개수입니다. 예: 20
        int size,
        // 조건에 맞는 전체 문서 개수입니다. 예: 42
        long totalCount
) {
}
