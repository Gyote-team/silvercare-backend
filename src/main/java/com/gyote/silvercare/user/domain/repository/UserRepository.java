package com.gyote.silvercare.user.domain.repository;

import com.gyote.silvercare.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKakaoId(String kakaoId);

    /** 탈퇴처럼 사용자 상태를 바꾸는 작업을 직렬화하기 위해 사용자 행을 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.kakaoId = :kakaoId")
    Optional<User> findByKakaoIdForUpdate(@Param("kakaoId") String kakaoId);

    /** 여러 사용자 행을 id 순서로 잠가 교착 상태 없이 탈퇴와 연결 변경을 직렬화한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id in :ids order by u.id")
    List<User> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);
}
