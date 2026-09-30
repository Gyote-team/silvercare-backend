package com.gyote.silvercare.user.api.dto.response;

import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MeResponseTest {

    @Test
    void kakaoAccountReportsKakaoProvider() {
        assertThat(MeResponse.of(user("4120839123"), null).loginProvider()).isEqualTo("KAKAO");
    }

    @Test
    void demoAccountReportsDemoProvider() {
        assertThat(MeResponse.of(user("demo-caregiver"), null).loginProvider()).isEqualTo("DEMO");
    }

    private static User user(String kakaoId) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKakaoId(kakaoId);
        user.setName("김민지");
        user.setRole(UserRole.CAREGIVER);
        return user;
    }
}
