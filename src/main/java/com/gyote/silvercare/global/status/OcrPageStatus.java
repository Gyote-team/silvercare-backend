package com.gyote.silvercare.global.status;

/**
 * 의료 문서의 개별 페이지 OCR 처리 상태입니다.
 * status값.html 공통 기준이며, 주로 AI-BACKEND(역할 B) OCR 파이프라인에서 사용합니다.
 */
public enum OcrPageStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    SKIPPED
}
