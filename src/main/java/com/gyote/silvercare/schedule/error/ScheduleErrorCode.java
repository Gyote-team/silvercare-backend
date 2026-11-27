package com.gyote.silvercare.schedule.error;

import com.gyote.silvercare.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

public enum ScheduleErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE_001", "일정을 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "SCHEDULE_002", "이 개인의 일정을 관리할 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "SCHEDULE_003", "일정이 변경되었습니다. 새로 불러온 뒤 다시 시도해 주세요.");
    private final HttpStatus status;
    private final String code;
    private final String message;

    ScheduleErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    /** 오류의 HTTP 상태를 반환한다. */
    public HttpStatus status() {
        return status;
    }

    /** 일정 도메인 오류 코드를 반환한다. */
    public String code() {
        return code;
    }

    /** 사용자에게 표시할 안내를 반환한다. */
    public String message() {
        return message;
    }
}
