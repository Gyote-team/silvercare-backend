package com.gyote.silvercare.global.status;

/** 음성 업로드부터 사용자 초안 확정까지의 상태입니다. */
public enum TranscriptionStatus {
    UPLOADED,
    PROCESSING,
    DRAFT_READY,
    CONFIRMED,
    FAILED,
    CANCELED
}
