package com.gyote.silvercare.medical_document.query.model;

/** 설명값과 추출 원문값을 대조하기 위한 조회 프로젝션입니다. */
public interface AiDocumentFactRow {

    /** 원문 추출 항목 식별자를 반환합니다. */
    String getFactId();

    /** 추출 항목의 유형을 반환합니다. */
    String getFactType();

    /** AI 설명에 사용된 표시값을 반환합니다. */
    String getDisplayValue();

    /** 원문에서 추출한 값을 반환합니다. */
    String getOriginalValue();

    /** 값의 단위를 반환합니다. */
    String getDisplayUnit();

    /** 원문에서 추출한 단위를 반환합니다. */
    String getOriginalUnit();

    /** 원문 인용 텍스트를 반환합니다. */
    String getSourceText();

    /** 원문이 위치한 페이지 순번을 반환합니다. */
    Integer getPageNo();

    /** 화면 강조용 앵커 키를 반환합니다. */
    String getAnchorId();
}
