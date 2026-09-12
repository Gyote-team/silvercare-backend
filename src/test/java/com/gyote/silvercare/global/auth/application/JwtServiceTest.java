package com.gyote.silvercare.global.auth.application;

import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void tokenRoundTripKeepsKakaoId() {
        JwtService jwt = new JwtService("test-secret-test-secret-test-secret-32", 30);
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKakaoId("kakao-soonja");
        user.setName("김순자");
        user.setRole(UserRole.PATIENT);
        user.setStatus(UserStatus.ACTIVE);

        String token = jwt.create(user);

        assertThat(token).isNotBlank();
        assertThat(jwt.kakaoId(token)).isEqualTo("kakao-soonja");
    }
}
