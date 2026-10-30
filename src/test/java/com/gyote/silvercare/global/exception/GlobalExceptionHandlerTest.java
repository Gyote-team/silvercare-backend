package com.gyote.silvercare.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyote.silvercare.user.error.UserErrorCode;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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
        var response =
                handler.handleFileTooLarge(
                        new MaxUploadSizeExceededException(1L),
                        new MockHttpServletRequest("POST", "/api/visits/test/documents"));

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody().code()).isEqualTo("MEDICAL_DOCUMENT_009");
    }

    @Test
    void oversizedVoiceMultipartKeepsVoiceErrorCode() {
        for (String path :
                new String[] {"/api/health-records/voice", "/api/visits/test/recordings"}) {
            var response =
                    handler.handleFileTooLarge(
                            new MaxUploadSizeExceededException(1L),
                            new MockHttpServletRequest("POST", path));
            assertThat(response.getStatusCode().value()).isEqualTo(413);
            assertThat(response.getBody().code()).isEqualTo("VOICE_004");
        }
    }

    @Test
    void missingRequestPartBecomesInvalidRequest() {
        var response =
                handler.handleInvalidMultipart(new MissingServletRequestPartException("file"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo("GLOBAL_002");
    }
}
