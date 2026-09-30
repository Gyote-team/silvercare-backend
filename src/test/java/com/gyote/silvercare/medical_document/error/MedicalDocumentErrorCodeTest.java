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
            "INVALID_PAGE_SIZE,      400, MEDICAL_DOCUMENT_006"
    })
    void businessExceptionKeepsMedicalDocumentStatusAndCode(
            MedicalDocumentErrorCode errorCode, int expectedStatus, String expectedCode
    ) {
        var response = handler.handleBusiness(new BusinessException(errorCode));

        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        assertThat(response.getBody().code()).isEqualTo(expectedCode);
    }
}
