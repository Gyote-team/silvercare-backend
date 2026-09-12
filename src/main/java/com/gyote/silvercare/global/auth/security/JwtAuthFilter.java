package com.gyote.silvercare.global.auth.security;

import com.gyote.silvercare.global.auth.application.AuthCookieService;
import com.gyote.silvercare.global.auth.application.JwtService;
import com.gyote.silvercare.user.query.application.UserQueryService;
import com.gyote.silvercare.user.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            filterChain.doFilter(request, response);
            return;
        }
        String token = cookies.read(request);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            User user = users.findByKakaoId(jwt.kakaoId(token)).orElse(null);
            if (user != null) {
                SecurityContextHolder.getContext().setAuthentication(principal(user));
            }
        } catch (RuntimeException ignored) {
            cookies.clear(response);
        }
        filterChain.doFilter(request, response);
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
