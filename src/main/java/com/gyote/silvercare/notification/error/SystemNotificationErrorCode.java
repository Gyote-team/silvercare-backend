package com.gyote.silvercare.notification.error;

import com.gyote.silvercare.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

/** 시스템 알림의 권한 및 조회 실패를 표현하는 공통 오류 코드. */
public enum SystemNotificationErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "SYSTEM_NOTIFICATION_001", "알림을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    SystemNotificationErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    /** 오류의 HTTP 상태를 반환한다. */
    @Override
    public HttpStatus status() {
        return status;
    }

    /** 클라이언트에서 구분할 오류 코드를 반환한다. */
    @Override
    public String code() {
        return code;
    }

    /** 사용자에게 표시할 오류 메시지를 반환한다. */
    @Override
    public String message() {
        return message;
    }
}
