package com.gyote.silvercare.health_record.error;
import com.gyote.silvercare.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum HealthRecordErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "HEALTH_RECORD_001", "건강기록을 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "HEALTH_RECORD_002", "이 개인의 건강기록에 접근할 수 없습니다."),
    AUTHOR_REQUIRED(HttpStatus.FORBIDDEN, "HEALTH_RECORD_003", "작성자만 수정하거나 삭제할 수 있습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "HEALTH_RECORD_004", "본문, 대상 개인 또는 페이지 정보를 확인해 주세요."),
    INVALID_VISIT(HttpStatus.BAD_REQUEST, "HEALTH_RECORD_005", "선택한 개인의 유효한 방문을 지정해 주세요."),
    CONFLICT(HttpStatus.CONFLICT, "HEALTH_RECORD_006", "다른 요청에서 기록을 변경했습니다. 다시 조회해 주세요.");
    private final HttpStatus status;
    private final String code;
    private final String message;
    HealthRecordErrorCode(HttpStatus status, String code, String message) {
        this.status=status; this.code=code; this.message=message;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String message() { return message; }
}
