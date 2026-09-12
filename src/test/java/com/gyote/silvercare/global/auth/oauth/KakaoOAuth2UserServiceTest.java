package com.gyote.silvercare.global.auth.oauth;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoOAuth2UserServiceTest {

    @Test
    void readsNumericKakaoIdAsString() {
        OAuth2User user = oauthUser(Map.of("id", 12345L));

        assertThat(KakaoOAuth2UserService.kakaoId(user)).isEqualTo("12345");
    }

    @Test
    void readsNicknameFromProperties() {
        OAuth2User user = oauthUser(Map.of(
                "id", 12345L,
                "properties", Map.of("nickname", "민지")
        ));

        assertThat(KakaoOAuth2UserService.nickname(user)).isEqualTo("민지");
    }

    @Test
    void readsNicknameFromKakaoAccountProfile() {
        OAuth2User user = oauthUser(Map.of(
                "id", 12345L,
                "kakao_account", Map.of("profile", Map.of("nickname", "김민지"))
        ));

        assertThat(KakaoOAuth2UserService.nickname(user)).isEqualTo("김민지");
    }

    @Test
    void fallsBackWhenNicknameMissing() {
        OAuth2User user = oauthUser(Map.of("id", 12345L));

        assertThat(KakaoOAuth2UserService.nickname(user)).isEqualTo("이용자");
    }

    private static OAuth2User oauthUser(Map<String, Object> attributes) {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );
    }
}
