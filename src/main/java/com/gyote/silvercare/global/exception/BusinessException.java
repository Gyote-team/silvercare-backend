package com.gyote.silvercare.global.exception;

/**
 * 도메인 규칙 위반을 API 오류 응답으로 변환하기 위한 공통 예외입니다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
