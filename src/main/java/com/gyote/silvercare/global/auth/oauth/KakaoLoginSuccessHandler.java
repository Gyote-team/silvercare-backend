package com.gyote.silvercare.global.auth.oauth;

import com.gyote.silvercare.global.auth.application.AuthCookieService;
import com.gyote.silvercare.global.config.FrontendOrigin;
import com.gyote.silvercare.user.query.application.UserQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class KakaoLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserQueryService users;
    private final AuthCookieService authCookies;
    private final FrontendOrigin frontend;

    public KakaoLoginSuccessHandler(
            UserQueryService users,
            AuthCookieService authCookies,
            FrontendOrigin frontend
    ) {
        this.users = users;
        this.authCookies = authCookies;
        this.frontend = frontend;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        if (authentication.getPrincipal() instanceof OAuth2User oauth) {
            Object kakaoId = oauth.getAttributes().get("id");
            if (kakaoId != null) {
                users.findByKakaoId(String.valueOf(kakaoId)).ifPresent(user -> authCookies.write(response, user));
            }
        }
        response.sendRedirect(frontend.path(needsRole(authentication) ? "/role" : "/home"));
    }

    public boolean needsRole(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User user)) {
            return false;
        }
        Object kakaoId = user.getAttributes().get("id");
        if (kakaoId == null) {
            return false;
        }
        return users.needsRole(String.valueOf(kakaoId));
    }
}
