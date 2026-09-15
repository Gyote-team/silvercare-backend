package com.gyote.silvercare.user.query.application;

import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.user.error.UserErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Read-only user lookup use cases. */
@Service
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository users;

    public UserQueryService(UserRepository users) {
        this.users = users;
    }

    public Optional<User> findByKakaoId(String kakaoId) {
        if (kakaoId == null || kakaoId.isBlank()) {
            return Optional.empty();
        }
        return users.findByKakaoId(kakaoId);
    }

    public User requireByKakaoId(String kakaoId) {
        return findByKakaoId(kakaoId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    public boolean needsRole(String kakaoId) {
        return findByKakaoId(kakaoId)
                .map(user -> user.getRole() == UserRole.PENDING)
                .orElse(false);
    }
}
