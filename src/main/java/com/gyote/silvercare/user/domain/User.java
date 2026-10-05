package com.gyote.silvercare.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    public static final String DEMO_KAKAO_ID_PREFIX = "demo-";
    private static final String LINKED_KEY_SEPARATOR = "#";

    @Id
    @Column(length = 36, nullable = false)
    private UUID id;

    @Column(length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "kakao_id", unique = true, length = 255)
    private String kakaoId;

    /** 같은 사람이 가진 개인·보호자 계정을 묶는 id. 처음 만든 계정의 id를 그대로 쓴다. */
    @Column(name = "account_group_id", nullable = false, length = 36)
    private UUID accountGroupId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (accountGroupId == null) {
            accountGroupId = id;
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getKakaoId() {
        return kakaoId;
    }

    public void setKakaoId(String kakaoId) {
        this.kakaoId = kakaoId;
    }

    public UUID getAccountGroupId() {
        return accountGroupId;
    }

    /** 처음 만든 계정이면 true. 카카오 로그인은 이 계정으로 들어온다. */
    public boolean isGroupOwner() {
        return id != null && id.equals(accountGroupId);
    }

    /**
     * 같은 사람의 다른 역할 계정을 만든다.
     * kakao_id는 계정마다 고유해야 하므로 처음 계정의 kakao_id 뒤에 역할을 붙인 값을 계정 키로 쓴다.
     */
    public static User linkedAccount(User owner, String name, UserRole role) {
        User linked = new User();
        linked.kakaoId = owner.getKakaoId() + LINKED_KEY_SEPARATOR + role.name();
        linked.accountGroupId = owner.getAccountGroupId();
        linked.name = name;
        linked.role = role;
        linked.status = UserStatus.ACTIVE;
        return linked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    /** 시연용 로그인으로 만든 계정은 kakao_id가 demo- 로 시작한다. */
    public boolean isDemoAccount() {
        return kakaoId != null && kakaoId.startsWith(DEMO_KAKAO_ID_PREFIX);
    }

    /**
     * 탈퇴 계정으로 전환하고 탈퇴 시각을 기록한다.
     * 같은 카카오 계정으로 새로 가입할 수 있도록 kakao_id 고유 제약에서 이 행을 풀어 준다.
     */
    public void withdraw(Instant withdrawnAt) {
        this.status = UserStatus.WITHDRAWN;
        this.deletedAt = withdrawnAt;
        this.kakaoId = null;
    }
}
