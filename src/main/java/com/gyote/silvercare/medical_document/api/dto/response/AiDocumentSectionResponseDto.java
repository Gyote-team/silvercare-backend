package com.gyote.silvercare.medical_document.api.dto.response;

import java.util.List;

public record AiDocumentSectionResponseDto(
        // 섹션의 고유 식별자입니다. 예: "section-001"
        String sectionId,
        // 섹션의 분류 코드입니다. 예: "VITAL_SIGNS"
        String sectionType,
        // 사용자에게 표시할 섹션 제목입니다. 예: "주요 건강 수치"
        String title,
        // 섹션에 포함된 핵심정보 항목 목록입니다. 예: [{"label":"혈압", "value":"120/80"}]
        List<AiDocumentSectionItemResponseDto> items
) {
}
