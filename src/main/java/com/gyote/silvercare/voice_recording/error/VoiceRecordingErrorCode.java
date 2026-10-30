package com.gyote.silvercare.voice_recording.error;

import com.gyote.silvercare.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

/** 음성 업로드·확정 및 처리 실패의 HTTP 계약. */
public enum VoiceRecordingErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "VOICE_001", "음성 기록을 찾을 수 없습니다."),
    INVALID_STATE(HttpStatus.CONFLICT, "VOICE_002", "현재 상태에서는 처리할 수 없습니다. 다시 불러와 주세요."),
    KEY_CONFLICT(HttpStatus.CONFLICT, "VOICE_003", "같은 요청 키로 다른 음성을 보낼 수 없습니다."),
    TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "VOICE_004", "음성 파일은 20MB 이하로 올려 주세요."),
    UNSUPPORTED(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE, "VOICE_005", "WAV·MP3·M4A·WebM·OGG 음성 파일을 선택해 주세요."),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "VOICE_006", "음성 변환을 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    private final HttpStatus status;
    private final String code;
    private final String message;

    VoiceRecordingErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    /** HTTP 상태를 반환한다. */
    public HttpStatus status() {
        return status;
    }

    /** 오류 코드를 반환한다. */
    public String code() {
        return code;
    }

    /** 사용자 안내를 반환한다. */
    public String message() {
        return message;
    }
}
