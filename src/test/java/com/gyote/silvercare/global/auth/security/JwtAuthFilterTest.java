package com.gyote.silvercare.global.auth.security;

import com.gyote.silvercare.global.auth.application.AuthCookieService;
import com.gyote.silvercare.global.auth.application.JwtService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.user.query.application.UserQueryService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private final JwtService jwt = new JwtService("local-test-secret-0123456789abcdef0123", 30);
    private final AuthCookieService cookies = new AuthCookieService(jwt, "SILVERCARE_TOKEN", 30, "SILVERCARE_SESSION");
    private final UserQueryService users = mock(UserQueryService.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwt, cookies, users);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenOfWithdrawnAccountDoesNotAuthenticateRejoinedAccount() throws Exception {
        User old = user("kakao-1");
        String oldToken = jwt.create(old);
        old.withdraw(Instant.now());
        User rejoined = user("kakao-1");
        when(users.findByKakaoId("kakao-1")).thenReturn(Optional.of(rejoined));

        MockHttpServletResponse response = run(oldToken);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getHeaders("Set-Cookie")).anyMatch(header -> header.startsWith("SILVERCARE_TOKEN=;"));
    }

    @Test
    void tokenOfCurrentAccountAuthenticates() throws Exception {
        User current = user("kakao-1");
        when(users.findByKakaoId("kakao-1")).thenReturn(Optional.of(current));

        run(jwt.create(current));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_CAREGIVER");
    }

    private MockHttpServletResponse run(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
        request.setCookies(new Cookie("SILVERCARE_TOKEN", token));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private static User user(String kakaoId) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKakaoId(kakaoId);
        user.setName("김민지");
        user.setRole(UserRole.CAREGIVER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
