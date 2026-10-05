package com.gyote.silvercare.global.exception;

import org.springframework.http.HttpStatus;

public enum GlobalErrorCode implements ErrorCode {
    DEMO_LOGIN_DISABLED(HttpStatus.FORBIDDEN, "GLOBAL_001", "데모 로그인이 비활성화되어 있습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "GLOBAL_002", "요청 값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL_003", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    GlobalErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
