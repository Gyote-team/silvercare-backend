package com.gyote.silvercare.medical_document.error;

import com.gyote.silvercare.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MedicalDocumentErrorCode implements ErrorCode {
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "MEDICAL_DOCUMENT_001", "문서를 찾을 수 없습니다."),
    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MEDICAL_DOCUMENT_002", "이 문서에 접근할 권한이 없습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "MEDICAL_DOCUMENT_003", "커서 값이 올바르지 않습니다."),
    PATIENT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "MEDICAL_DOCUMENT_004", "조회할 개인을 지정해야 합니다."),
    DOCUMENT_ALREADY_DELETED(HttpStatus.CONFLICT, "MEDICAL_DOCUMENT_005", "이미 삭제된 문서입니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "MEDICAL_DOCUMENT_006", "size는 1 이상 50 이하여야 합니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "MEDICAL_DOCUMENT_007", "빈 파일은 업로드할 수 없습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "MEDICAL_DOCUMENT_008", "JPEG, PNG, PDF 파일만 업로드할 수 있습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "MEDICAL_DOCUMENT_009", "파일 크기가 허용 한도를 넘었습니다."),
    PDF_PAGE_LIMIT_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "MEDICAL_DOCUMENT_010", "PDF 페이지 수가 허용 한도를 넘었습니다."),
    IMAGE_RESOLUTION_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "MEDICAL_DOCUMENT_011", "이미지 해상도가 허용 한도를 넘었습니다."),
    VISIT_NOT_FOUND(HttpStatus.NOT_FOUND, "MEDICAL_DOCUMENT_012", "방문을 찾을 수 없습니다."),
    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "MEDICAL_DOCUMENT_013", "Idempotency-Key는 1자 이상 100자 이하여야 합니다."),
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "MEDICAL_DOCUMENT_014", "같은 Idempotency-Key가 다른 방문의 업로드에 이미 사용됐습니다."),
    INVALID_DOC_TYPE(HttpStatus.BAD_REQUEST, "MEDICAL_DOCUMENT_015", "문서 유형 값이 올바르지 않습니다."),
    STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "MEDICAL_DOCUMENT_016", "파일 저장소를 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    ENCRYPTED_PDF(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "MEDICAL_DOCUMENT_017", "비밀번호가 걸린 PDF는 올릴 수 없습니다. 비밀번호를 해제한 뒤 다시 올려 주세요.");

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