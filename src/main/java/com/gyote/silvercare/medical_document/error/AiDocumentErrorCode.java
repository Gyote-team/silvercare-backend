package com.gyote.silvercare.medical_document.error;

import com.gyote.silvercare.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AiDocumentErrorCode implements ErrorCode {
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_DOCUMENT_001", "의료문서를 찾을 수 없습니다."),
    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AI_DOCUMENT_002", "의료문서를 조회할 권한이 없습니다."),
    PATIENT_REQUIRED(HttpStatus.BAD_REQUEST, "AI_DOCUMENT_003", "보호자 조회에는 patientId가 필요합니다."),
    ANALYSIS_NOT_COMPLETED(HttpStatus.CONFLICT, "AI_DOCUMENT_004", "문서 분석이 아직 완료되지 않았습니다."),
    EXPLANATION_NOT_READY(HttpStatus.CONFLICT, "AI_DOCUMENT_005", "AI 설명이 아직 준비되지 않았습니다."),
    INVALID_SECTION_TYPE(HttpStatus.BAD_REQUEST, "AI_DOCUMENT_006", "지원하지 않는 설명 섹션입니다."),
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "AI_DOCUMENT_007", "페이지 요청 값이 올바르지 않습니다."),
    INVALID_FILTER(HttpStatus.BAD_REQUEST, "AI_DOCUMENT_008", "문서 조회 조건이 올바르지 않습니다."),
    PAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_DOCUMENT_009", "요청한 문서 페이지를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AiDocumentErrorCode(HttpStatus status, String code, String message) {
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
