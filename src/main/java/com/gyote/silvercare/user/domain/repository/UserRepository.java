package com.gyote.silvercare.user.domain.repository;

import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
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

    /** 같은 사람의 활성 계정을 역할 순서로 반환한다. */
    List<User> findByAccountGroupIdAndStatusOrderByRoleAsc(UUID accountGroupId, UserStatus status);

    /** 같은 사람이 가진 특정 역할의 활성 계정을 찾는다. */
    Optional<User> findByAccountGroupIdAndRoleAndStatus(UUID accountGroupId, UserRole role, UserStatus status);

    /** 같은 사람의 계정을 id 순서로 모두 잠근다. 계정 전환·탈퇴를 직렬화할 때 쓴다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.accountGroupId = :accountGroupId order by u.id")
    List<User> findAllByAccountGroupIdForUpdate(@Param("accountGroupId") UUID accountGroupId);

    /** 여러 사용자 행을 id 순서로 잠가 교착 상태 없이 탈퇴와 연결 변경을 직렬화한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id in :ids order by u.id")
    List<User> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);
}
