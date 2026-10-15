package com.gyote.silvercare.ai_result.error;

import com.gyote.silvercare.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** AI 답변 문서 조회 오류를 정의한다. */
public enum AiResultErrorCode implements ErrorCode {
    RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_RESULT_001", "AI 답변 문서를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AiResultErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    /** 오류에 대응하는 HTTP 상태를 반환한다. */
    @Override
    public HttpStatus status() {
        return status;
    }

    /** 클라이언트 계약용 오류 코드를 반환한다. */
    @Override
    public String code() {
        return code;
    }

    /** 클라이언트에 전달할 오류 메시지를 반환한다. */
    @Override
    public String message() {
        return message;
    }
}
