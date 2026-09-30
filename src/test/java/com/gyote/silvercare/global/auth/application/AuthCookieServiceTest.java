package com.gyote.silvercare.global.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTest {

    @Test
    void clearAllExpiresJwtAndSessionCookies() {
        AuthCookieService cookies = new AuthCookieService(null, "SILVERCARE_TOKEN", 30, "SILVERCARE_SESSION");
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookies.clearAll(response);

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header).startsWith("SILVERCARE_TOKEN=;").contains("Max-Age=0", "Path=/"))
                .anySatisfy(header -> assertThat(header).startsWith("SILVERCARE_SESSION=;").contains("Max-Age=0", "Path=/"));
    }
}
