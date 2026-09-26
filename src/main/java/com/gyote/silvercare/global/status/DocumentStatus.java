package com.gyote.silvercare.global.status;

/** 문서 저장·AI 분석 결과의 문서 단위 상태입니다. */
public enum DocumentStatus {
    UPLOADED,
    PROCESSING,
    READY,
    NEEDS_REVIEW,
    FAILED,
    DELETED
}
