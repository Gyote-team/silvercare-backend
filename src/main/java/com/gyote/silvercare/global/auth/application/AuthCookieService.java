package com.gyote.silvercare.global.auth.application;

import com.gyote.silvercare.user.domain.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    private final JwtService jwt;
    private final String cookieName;
    private final long ttlDays;
    private final String sessionCookieName;

    public AuthCookieService(
            JwtService jwt,
            @Value("${jwt.cookie-name:SILVERCARE_TOKEN}") String cookieName,
            @Value("${jwt.ttl-days:30}") long ttlDays,
            @Value("${server.servlet.session.cookie.name:JSESSIONID}") String sessionCookieName
    ) {
        this.jwt = jwt;
        this.cookieName = cookieName;
        this.ttlDays = ttlDays;
        this.sessionCookieName = sessionCookieName;
    }

    public void write(HttpServletResponse response, User user) {
        attach(response, cookieName, jwt.create(user), ttlDays * 24 * 3600);
    }

    public void clear(HttpServletResponse response) {
        attach(response, cookieName, "", 0);
    }

    /** JWT 쿠키와 함께 브라우저의 세션 쿠키도 만료시킨다. session.invalidate()는 세션 쿠키를 지우지 않는다. */
    public void clearAll(HttpServletResponse response) {
        clear(response);
        attach(response, sessionCookieName, "", 0);
    }

    public String read(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length()).trim();
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void attach(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
