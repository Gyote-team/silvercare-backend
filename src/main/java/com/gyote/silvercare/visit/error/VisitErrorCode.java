package com.gyote.silvercare.visit.error;

import com.gyote.silvercare.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

/** 방문 관리의 조회·권한·상태 규칙 위반 코드. */
public enum VisitErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "VISIT_001", "방문을 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "VISIT_002", "이 개인의 방문에 접근할 수 없습니다."),
    INVALID_DATE_STATUS(HttpStatus.BAD_REQUEST, "VISIT_003", "미래 방문은 예약 상태로만 등록하거나 수정할 수 있습니다."),
    CANCELED_IMMUTABLE(HttpStatus.CONFLICT, "VISIT_004", "취소된 방문은 수정할 수 없습니다.");
    private final HttpStatus status;
    private final String code;
    private final String message;

    VisitErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    /** HTTP 오류 상태를 반환한다. */
    @Override
    public HttpStatus status() {
        return status;
    }

    /** 기계 판독용 오류 코드를 반환한다. */
    @Override
    public String code() {
        return code;
    }

    /** 사용자 안내 문구를 반환한다. */
    @Override
    public String message() {
        return message;
    }
}
