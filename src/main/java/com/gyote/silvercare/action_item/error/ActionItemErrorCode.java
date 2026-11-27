package com.gyote.silvercare.action_item.error;

import com.gyote.silvercare.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

/** 할 일 후보 조회 시 사용하는 도메인 오류다. */
public enum ActionItemErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "ACTION_ITEM_001", "할 일 후보를 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACTION_ITEM_002", "이 개인의 할 일 후보를 관리할 수 없습니다."),
    STATE_CONFLICT(
            HttpStatus.CONFLICT, "ACTION_ITEM_003", "이미 처리되었거나 원본 문서·방문이 취소된 후보입니다. 새로 불러와 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ActionItemErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    /** 오류에 대응하는 HTTP 상태를 반환한다. */
    @Override
    public HttpStatus status() {
        return status;
    }

    /** 클라이언트가 구분할 도메인 오류 코드를 반환한다. */
    @Override
    public String code() {
        return code;
    }

    /** 사용자에게 표시할 오류 설명을 반환한다. */
    @Override
    public String message() {
        return message;
    }
}
