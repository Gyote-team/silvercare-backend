package com.gyote.silvercare.user.command.application;

import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class UserAccountService {

    private static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;

    public UserAccountService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public User loginOrRegister(String kakaoId, String nickname) {
        if (kakaoId == null || kakaoId.isBlank()) {
            throw new IllegalArgumentException("kakao_id가 없습니다");
        }
        String name = (nickname == null || nickname.isBlank()) ? "이용자" : nickname.trim();
        return users.findByKakaoId(kakaoId).map(existing -> {
            existing.setName(name);
            return existing;
        }).orElseGet(() -> {
            User created = new User();
            created.setKakaoId(kakaoId);
            created.setName(name);
            created.setRole(UserRole.PENDING);
            created.setStatus(UserStatus.ACTIVE);
            return users.save(created);
        });
    }

    @Transactional
    public User chooseRole(String kakaoId, UserRole role) {
        if (role != UserRole.PATIENT && role != UserRole.CAREGIVER) {
            throw new IllegalArgumentException("역할은 개인 또는 보호자만 고를 수 있습니다");
        }
        User user = users.findByKakaoId(kakaoId)
                .orElseThrow(() -> new IllegalArgumentException("카카오 계정이 없습니다"));
        if (user.getRole() != UserRole.PENDING) {
            throw new IllegalStateException("역할은 한 번만 고를 수 있습니다");
        }
        user.setRole(role);
        if (role == UserRole.PATIENT) {
            ensureInviteCode(user);
        }
        return user;
    }

    @Transactional
    public User ensureInviteCode(User user) {
        if (user.getRole() != UserRole.PATIENT) {
            return user;
        }
        if (user.getInviteCode() != null && !user.getInviteCode().isBlank()) {
            return user;
        }
        user.setInviteCode(newInviteCode());
        return user;
    }

    private String newInviteCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder code = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                code.append(INVITE_ALPHABET.charAt(RANDOM.nextInt(INVITE_ALPHABET.length())));
            }
            String candidate = code.toString();
            if (users.findByInviteCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("초대 코드를 만들 수 없습니다");
    }

    @Transactional
    public User ensureDemoUser(UserRole role) {
        if (role != UserRole.PATIENT && role != UserRole.CAREGIVER) {
            throw new IllegalArgumentException("역할은 개인 또는 보호자만 고를 수 있습니다");
        }
        String kakaoId = role == UserRole.CAREGIVER ? "demo-caregiver" : "demo-patient";
        String name = role == UserRole.CAREGIVER ? "김민지" : "김순자";
        User user = users.findByKakaoId(kakaoId).orElseGet(() -> {
            User created = new User();
            created.setKakaoId(kakaoId);
            created.setName(name);
            created.setRole(role);
            created.setStatus(UserStatus.ACTIVE);
            return users.save(created);
        });
        user.setName(name);
        if (user.getRole() == UserRole.PENDING) {
            user.setRole(role);
        }
        if (role == UserRole.PATIENT) {
            ensureInviteCode(user);
        }
        return user;
    }
}
