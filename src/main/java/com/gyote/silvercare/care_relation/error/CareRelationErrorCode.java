package com.gyote.silvercare.care_relation.error;

import com.gyote.silvercare.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CareRelationErrorCode implements ErrorCode {
    CAREGIVER_ONLY(HttpStatus.FORBIDDEN, "CARE_RELATION_001", "보호자만 연결을 요청할 수 있습니다."),
    INVITE_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "CARE_RELATION_002", "초대 코드를 찾을 수 없습니다."),
    SELF_RELATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CARE_RELATION_003", "자기 자신과는 연결할 수 없습니다."),
    RELATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "CARE_RELATION_004", "이미 요청했거나 연결되어 있습니다."),
    RELATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CARE_RELATION_005", "연결을 찾을 수 없습니다."),
    INVALID_RELATION_STATE(HttpStatus.CONFLICT, "CARE_RELATION_006", "현재 연결 상태에서는 요청을 처리할 수 없습니다."),
    RELATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CARE_RELATION_007", "이 연결을 처리할 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CareRelationErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
