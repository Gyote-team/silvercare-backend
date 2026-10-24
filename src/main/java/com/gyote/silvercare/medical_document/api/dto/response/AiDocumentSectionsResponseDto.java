package com.gyote.silvercare.medical_document.api.dto.response;

import java.util.List;

public record AiDocumentSectionsResponseDto(
        // 섹션을 조회한 문서의 ID입니다. 예: "doc-001"
        String documentId,
        // 문서의 핵심정보 섹션 목록입니다. 예: [{"sectionType":"VITAL_SIGNS", ...}]
        List<AiDocumentSectionResponseDto> sections
) {
}
