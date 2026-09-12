package com.gyote.silvercare.user.domain.repository;

import com.gyote.silvercare.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKakaoId(String kakaoId);

    Optional<User> findByInviteCode(String inviteCode);
}
