package com.gyote.silvercare.global.status;

/** 의료 문서의 개별 페이지 OCR 처리 상태입니다. */
public enum OcrPageStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    SKIPPED
}
