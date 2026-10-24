package com.gyote.silvercare.ai_chat.error;

import com.gyote.silvercare.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** AI 채팅방 조회와 사용 권한 오류를 정의한다. */
public enum AiChatErrorCode implements ErrorCode {
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AI_CHAT_001", "채팅방에 접근할 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AiChatErrorCode(HttpStatus status, String code, String message) {
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
