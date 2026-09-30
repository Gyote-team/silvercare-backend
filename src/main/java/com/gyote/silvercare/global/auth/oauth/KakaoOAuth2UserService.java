package com.gyote.silvercare.global.auth.oauth;

import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {

    private final UserAccountService accounts;

    public KakaoOAuth2UserService(UserAccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        String kakaoId = kakaoId(oauthUser);
        User user = accounts.loginOrRegister(kakaoId, nickname(oauthUser));

        Map<String, Object> attributes = new HashMap<>(oauthUser.getAttributes());
        attributes.put("id", kakaoId);
        attributes.put("userId", user.getId().toString());
        attributes.put("displayName", user.getName());
        attributes.put("role", user.getRole().name());

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                "id"
        );
    }

    static String kakaoId(OAuth2User oauthUser) {
        Object id = oauthUser.getAttributes().get("id");
        if (id == null) {
            throw new OAuth2AuthenticationException("kakao_id가 없습니다");
        }
        return String.valueOf(id);
    }

    @SuppressWarnings("unchecked")
    static String nickname(OAuth2User oauthUser) {
        Object properties = oauthUser.getAttribute("properties");
        if (properties instanceof Map<?, ?> map && map.get("nickname") != null) {
            return String.valueOf(map.get("nickname"));
        }
        Object account = oauthUser.getAttribute("kakao_account");
        if (account instanceof Map<?, ?> accountMap) {
            Object profile = accountMap.get("profile");
            if (profile instanceof Map<?, ?> profileMap && profileMap.get("nickname") != null) {
                return String.valueOf(profileMap.get("nickname"));
            }
        }
        return "이용자";
    }
}
