package com.gyote.silvercare.global.exception;

import com.gyote.silvercare.user.error.UserErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionKeepsDomainStatusAndCode() {
        var response = handler.handleBusiness(new BusinessException(UserErrorCode.USER_NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND.status());
        assertThat(response.getBody().code()).isEqualTo("USER_003");
    }
}
