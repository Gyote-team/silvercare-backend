package com.gyote.silvercare.medical_document.api.dto.response;

public record AiDocumentSectionItemResponseDto(
        // 설명 문장 또는 항목의 고유 식별자입니다. 예: "sentence-001"
        String sentenceId,
        // 원문에서 추출된 항목의 ID입니다. 예: "source-item-001"
        String sourceItemId,
        // 항목의 표시 이름입니다. 예: "혈압"
        String label,
        // 항목의 값입니다. 예: "120/80"
        String value,
        // 값의 단위입니다. 해당하지 않으면 null입니다. 예: "mmHg"
        String unit,
        // 원문 근거가 연결되어 있는지 여부입니다. 예: true
        boolean hasSource,
        // 연결된 원문 근거의 ID입니다. 근거가 없으면 null입니다. 예: "citation-001"
        String citationId
) {
}
