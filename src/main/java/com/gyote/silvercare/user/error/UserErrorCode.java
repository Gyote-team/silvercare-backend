package com.gyote.silvercare.user.error;

import com.gyote.silvercare.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {
    KAKAO_ID_REQUIRED(HttpStatus.BAD_REQUEST, "USER_001", "카카오 식별자가 필요합니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "USER_002", "개인 또는 보호자 역할만 선택할 수 있습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_003", "사용자를 찾을 수 없습니다."),
    ROLE_ALREADY_SELECTED(HttpStatus.CONFLICT, "USER_004", "역할은 한 번만 선택할 수 있습니다."),
    INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_005", "초대 코드를 생성할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    UserErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
