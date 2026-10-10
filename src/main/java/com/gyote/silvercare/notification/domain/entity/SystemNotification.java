package com.gyote.silvercare.notification.domain.entity;

import com.gyote.silvercare.notification.domain.SystemActivityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** 역할 계정별 시스템 활동 알림과 읽음 상태를 보존한다. */
@Entity
@Table(name = "system_notifications")
@Getter
@NoArgsConstructor
public class SystemNotification {
    @Id
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SystemActivityType type;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String path;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    public SystemNotification(
            UUID recipient, SystemActivityType type, String message, String path) {
        id = UUID.randomUUID();
        recipientUserId = recipient;
        this.type = type;
        this.message = message;
        this.path = path;
        createdAt = Instant.now();
    }

    /** 자신의 알림을 읽음 처리한다. 재요청도 같은 결과를 유지한다. */
    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}
