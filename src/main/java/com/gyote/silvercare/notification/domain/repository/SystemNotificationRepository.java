package com.gyote.silvercare.notification.domain.repository;

import com.gyote.silvercare.notification.domain.entity.SystemNotification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 수신자 기준으로 시스템 알림과 미읽음 수를 조회한다. */
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, UUID> {
    List<SystemNotification> findByRecipientUserIdOrderByCreatedAtDescIdDesc(
            UUID recipient, Pageable page);

    Optional<SystemNotification> findByIdAndRecipientUserId(UUID id, UUID recipient);

    long countByRecipientUserIdAndReadAtIsNull(UUID recipient);

    List<SystemNotification> findByRecipientUserIdAndReadAtIsNull(UUID recipient);
}
