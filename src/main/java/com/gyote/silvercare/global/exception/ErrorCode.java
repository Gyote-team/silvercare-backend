package com.gyote.silvercare.global.exception;

import org.springframework.http.HttpStatus;

/**
 * API 오류의 HTTP 상태, 기계 판독용 코드, 사용자 메시지를 정의하는 공통 계약입니다.
 */
public interface ErrorCode {

    HttpStatus status();

    String code();

    String message();
}
