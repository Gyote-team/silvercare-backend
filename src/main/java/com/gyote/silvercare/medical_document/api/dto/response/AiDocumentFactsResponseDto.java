package com.gyote.silvercare.medical_document.api.dto.response;
import java.util.List;

/** 문서의 AI 설명값과 원문값 대조 결과입니다. */
public record AiDocumentFactsResponseDto(
        String documentId,
        List<FactItem> items
) {

    /** 개별 검증 항목입니다. */
    public record FactItem(
            String factId,
            String factType,
            String displayValue,
            String originalValue,
            String displayUnit,
            String originalUnit,
            String validationStatus,
            Integer pageNo,
            String sourceText,
            String anchorId
    ) {
    }
}
