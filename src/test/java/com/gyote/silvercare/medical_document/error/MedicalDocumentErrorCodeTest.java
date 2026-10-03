package com.gyote.silvercare.medical_document.error;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalDocumentErrorCodeTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest
    @CsvSource({
            "DOCUMENT_NOT_FOUND,     404, MEDICAL_DOCUMENT_001",
            "DOCUMENT_ACCESS_DENIED, 403, MEDICAL_DOCUMENT_002",
            "INVALID_CURSOR,         400, MEDICAL_DOCUMENT_003",
            "PATIENT_ID_REQUIRED,    400, MEDICAL_DOCUMENT_004",
            "DOCUMENT_ALREADY_DELETED, 409, MEDICAL_DOCUMENT_005",
            "INVALID_PAGE_SIZE,      400, MEDICAL_DOCUMENT_006",
            "EMPTY_FILE,                400, MEDICAL_DOCUMENT_007",
            "UNSUPPORTED_FILE_TYPE,     415, MEDICAL_DOCUMENT_008",
            "FILE_TOO_LARGE,            413, MEDICAL_DOCUMENT_009",
            "PDF_PAGE_LIMIT_EXCEEDED,   413, MEDICAL_DOCUMENT_010",
            "IMAGE_RESOLUTION_EXCEEDED, 413, MEDICAL_DOCUMENT_011",
            "VISIT_NOT_FOUND,           404, MEDICAL_DOCUMENT_012",
            "INVALID_IDEMPOTENCY_KEY,   400, MEDICAL_DOCUMENT_013",
            "IDEMPOTENCY_KEY_CONFLICT,  409, MEDICAL_DOCUMENT_014",
            "INVALID_DOC_TYPE,          400, MEDICAL_DOCUMENT_015",
            "STORAGE_UNAVAILABLE,       503, MEDICAL_DOCUMENT_016"
    })
    void businessExceptionKeepsMedicalDocumentStatusAndCode(
            MedicalDocumentErrorCode errorCode, int expectedStatus, String expectedCode
    ) {
        var response = handler.handleBusiness(new BusinessException(errorCode));

        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        assertThat(response.getBody().code()).isEqualTo(expectedCode);
    }
}
