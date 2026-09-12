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

    public AuthCookieService(
            JwtService jwt,
            @Value("${jwt.cookie-name:SILVERCARE_TOKEN}") String cookieName,
            @Value("${jwt.ttl-days:30}") long ttlDays
    ) {
        this.jwt = jwt;
        this.cookieName = cookieName;
        this.ttlDays = ttlDays;
    }

    public void write(HttpServletResponse response, User user) {
        attach(response, jwt.create(user), ttlDays * 24 * 3600);
    }

    public void clear(HttpServletResponse response) {
        attach(response, "", 0);
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

    private void attach(HttpServletResponse response, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
