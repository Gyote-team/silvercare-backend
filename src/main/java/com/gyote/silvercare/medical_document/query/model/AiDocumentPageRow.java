package com.gyote.silvercare.medical_document.query.model;

/** 페이지 렌더링 파일과 원본 문서 폴백 정보를 담는 조회 프로젝션입니다. */
public interface AiDocumentPageRow {

    /** 페이지 식별자를 반환합니다. */
    String getPageId();

    /** 문서 안의 페이지 순번을 반환합니다. */
    Integer getPageNo();

    /** 서명 URL 생성에 사용할 객체 저장소 키를 반환합니다. */
    String getStorageKey();

    /** 페이지별 렌더링 파일인지 여부를 반환합니다. */
    Boolean getRenderedPage();

    /** 선택한 원문 앵커의 좌표 JSON을 반환합니다. */
    String getSourceBox();

    /** 원본 페이지의 너비를 픽셀 단위로 반환합니다. */
    Integer getPageWidthPx();

    /** 원본 페이지의 높이를 픽셀 단위로 반환합니다. */
    Integer getPageHeightPx();
}
