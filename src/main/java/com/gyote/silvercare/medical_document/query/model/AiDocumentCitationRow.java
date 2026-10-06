package com.gyote.silvercare.medical_document.query.model;

/** 설명 문장과 원문 페이지를 연결하기 위한 인용 조회 프로젝션입니다. */
public interface AiDocumentCitationRow {

    /** 인용 식별자를 반환합니다. */
    String getCitationId();

    /** 설명 섹션 식별자를 반환합니다. */
    String getSectionId();

    /** 설명 문장 식별자를 반환합니다. */
    String getSentenceId();

    /** 원문 추출 항목 식별자를 반환합니다. */
    String getSourceItemId();

    /** 원문 청크 식별자를 반환합니다. */
    String getChunkId();

    /** 원문 페이지 식별자를 반환합니다. */
    String getPageId();

    /** 원문 페이지 순번을 반환합니다. */
    Integer getPageNo();

    /** 인용할 원문 텍스트를 반환합니다. */
    String getSourceText();

    /** 원문 강조 영역 좌표를 반환합니다. */
    String getSourceBox();

    /** 페이지 너비를 픽셀 단위로 반환합니다. */
    Integer getPageWidthPx();

    /** 페이지 높이를 픽셀 단위로 반환합니다. */
    Integer getPageHeightPx();

    /** 화면 강조용 앵커 키를 반환합니다. */
    String getAnchorId();
}
