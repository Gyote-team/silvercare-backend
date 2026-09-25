package com.gyote.silvercare.global.status;

/** AI 작업 실행 자체의 상태입니다. */
public enum AiJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED
}
