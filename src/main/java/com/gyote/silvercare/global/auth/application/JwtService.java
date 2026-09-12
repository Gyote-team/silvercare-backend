package com.gyote.silvercare.global.auth.application;

import com.gyote.silvercare.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlDays;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.ttl-days:30}") long ttlDays
    ) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET은 32자 이상이어야 합니다");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttlDays = ttlDays;
    }

    public String create(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getKakaoId())
                .claim("uid", user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlDays * 24 * 3600)))
                .signWith(key)
                .compact();
    }

    public String kakaoId(String token) {
        return claims(token).getSubject();
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
