package com.gyote.silvercare.global.status;

/** AI 작업 실행 자체의 상태입니다. */
public enum AiJobStatus {
    /** AI 작업이 큐에 등록되어 실행을 기다리는 상태입니다. */
    QUEUED,
    /** AI 작업이 현재 실행 중인 상태입니다. */
    RUNNING,
    /** AI 작업이 정상적으로 완료된 상태입니다. */
    SUCCEEDED,
    /** AI 작업이 오류로 실패한 상태입니다. */
    FAILED,
    /** AI 작업이 취소된 상태입니다. */
    CANCELED
}
