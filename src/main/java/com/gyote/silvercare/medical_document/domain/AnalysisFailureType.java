package com.gyote.silvercare.medical_document.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** AI 서버에 분석 시작을 요청하다 실패한 이유입니다. 값마다 재시도 가능 여부와 기록할 메시지를 가집니다. */
@Getter
@RequiredArgsConstructor
public enum AnalysisFailureType {

    /** 응답 시간 초과 */
    AI_TIMEOUT(true, "AI 서버 응답 시간이 초과되었습니다."),

    /** 연결 실패, 5xx, 429 */
    AI_UNAVAILABLE(true, "AI 서버에 연결할 수 없거나 서버 오류가 발생했습니다."),

    /** 401, 403 */
    INTERNAL_AUTH_FAILED(false, "AI 서버 내부 인증에 실패했습니다."),

    /** 그 외 4xx */
    AI_REQUEST_REJECTED(false, "AI 서버가 분석 요청을 거절했습니다.");

    private final boolean retryable;
    private final String message;
}
