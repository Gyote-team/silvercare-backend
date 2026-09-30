package com.gyote.silvercare.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void cancelMapsToCanceled() {
        OAuth2AuthenticationException denied = new OAuth2AuthenticationException(
                new OAuth2Error("access_denied")
        );
        assertThat(SecurityConfig.loginError(denied)).isEqualTo("canceled");
    }

    @Test
    void otherFailureMapsToFail() {
        OAuth2AuthenticationException other = new OAuth2AuthenticationException(
                new OAuth2Error("server_error")
        );
        assertThat(SecurityConfig.loginError(other)).isEqualTo("fail");
        assertThat(SecurityConfig.loginError(new IllegalStateException("broken"))).isEqualTo("fail");
    }
}
