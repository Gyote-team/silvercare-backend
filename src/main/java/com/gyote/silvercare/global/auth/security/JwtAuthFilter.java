package com.gyote.silvercare.global.auth.security;

import com.gyote.silvercare.global.auth.application.AuthCookieService;
import com.gyote.silvercare.global.auth.application.JwtService;
import com.gyote.silvercare.user.query.application.UserQueryService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final AuthCookieService cookies;
    private final UserQueryService users;

    public JwtAuthFilter(JwtService jwt, AuthCookieService cookies, UserQueryService users) {
        this.jwt = jwt;
        this.cookies = cookies;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current != null && current.isAuthenticated()
                && !(current.getPrincipal() instanceof String)) {
            if (current.getPrincipal() instanceof OAuth2User oauthUser
                    && activeUser(oauthUser.getAttribute("id"), oauthUser.getAttribute("userId")).isPresent()) {
                filterChain.doFilter(request, response);
                return;
            }
            SecurityContextHolder.clearContext();
            cookies.clear(response);
        }
        String token = cookies.read(request);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Optional<User> user = activeUser(jwt.kakaoId(token), jwt.userId(token));
            if (user.isPresent()) {
                SecurityContextHolder.getContext().setAuthentication(principal(user.get()));
            } else {
                cookies.clear(response);
            }
        } catch (RuntimeException ignored) {
            cookies.clear(response);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 탈퇴 후 같은 카카오 계정으로 재가입할 수 있으므로, 카카오 id만으로는 옛 계정의 세션·토큰을 구분할 수 없다.
     * 카카오 id와 사용자 id가 모두 현재 활성 계정과 일치할 때만 인증한다.
     */
    private Optional<User> activeUser(Object kakaoId, Object userId) {
        if (kakaoId == null || userId == null) {
            return Optional.empty();
        }
        return users.findByKakaoId(String.valueOf(kakaoId))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .filter(user -> user.getId().toString().equals(String.valueOf(userId)));
    }

    public static OAuth2AuthenticationToken principal(User user) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", user.getKakaoId());
        attributes.put("userId", user.getId().toString());
        attributes.put("displayName", user.getName());
        attributes.put("role", user.getRole().name());
        DefaultOAuth2User oauth = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                "id"
        );
        return new OAuth2AuthenticationToken(oauth, oauth.getAuthorities(), "kakao");
    }
}
