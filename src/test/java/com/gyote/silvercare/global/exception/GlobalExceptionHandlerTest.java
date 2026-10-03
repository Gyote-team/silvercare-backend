package com.gyote.silvercare.global.exception;

import com.gyote.silvercare.user.error.UserErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionKeepsDomainStatusAndCode() {
        var response = handler.handleBusiness(new BusinessException(UserErrorCode.USER_NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND.status());
        assertThat(response.getBody().code()).isEqualTo("USER_003");
    }

    @Test
    void maxUploadSizeExceededBecomesFileTooLarge() {
        var response = handler.handleFileTooLarge(new MaxUploadSizeExceededException(1L));

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody().code()).isEqualTo("GLOBAL_004");
    }

    @Test
    void missingRequestPartBecomesInvalidRequest() {
        var response = handler.handleInvalidMultipart(new MissingServletRequestPartException("file"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo("GLOBAL_002");
    }
}
