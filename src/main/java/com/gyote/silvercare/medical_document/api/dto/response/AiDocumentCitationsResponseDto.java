package com.gyote.silvercare.medical_document.api.dto.response;
import java.util.List;

/** 문서의 AI 설명과 원문 위치를 연결하는 인용 응답입니다. */
public record AiDocumentCitationsResponseDto(
        String documentId,
        List<CitationItem> citations
) {

    /** 개별 원문 인용입니다. */
    public record CitationItem(
            String citationId,
            String sectionId,
            String sentenceId,
            String sourceItemId,
            String chunkId,
            String pageId,
            Integer pageNo,
            String sourceText,
            SourceBox sourceBox,
            Integer pageWidthPx,
            Integer pageHeightPx,
            String anchorId
    ) {
    }

    /** 원문 페이지에서 강조할 사각형 좌표입니다. */
    public record SourceBox(
            Integer x,
            Integer y,
            Integer width,
            Integer height
    ) {
    }
}
