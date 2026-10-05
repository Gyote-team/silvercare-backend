package com.gyote.silvercare.health_record.error;

import com.gyote.silvercare.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum HealthRecordErrorCode implements ErrorCode {
    RECORD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "HEALTH_RECORD_001", "이 개인의 건강 기록에 접근할 권한이 없습니다."),
    PATIENT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "HEALTH_RECORD_002", "개인 식별자가 필요합니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "HEALTH_RECORD_003", "조회 건수는 1~50 사이여야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    HealthRecordErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
