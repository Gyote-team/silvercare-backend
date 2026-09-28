package com.gyote.silvercare.medical_document.error;

import com.gyote.silvercare.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MedicalDocumentErrorCode implements ErrorCode {
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "MEDICAL_DOCUMENT_001", "문서를 찾을 수 없습니다."),
    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MEDICAL_DOCUMENT_002", "이 문서에 접근할 권한이 없습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "MEDICAL_DOCUMENT_003", "커서 값이 올바르지 않습니다."),
    PATIENT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "MEDICAL_DOCUMENT_004", "조회할 개인을 지정해야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    MedicalDocumentErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}