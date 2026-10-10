package com.gyote.silvercare.notification.domain.repository;
import com.gyote.silvercare.notification.domain.entity.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface SystemNotificationRepository extends JpaRepository<SystemNotification,UUID> {
    List<SystemNotification> findByRecipientUserIdOrderByCreatedAtDescIdDesc(UUID recipient,Pageable page);
    Optional<SystemNotification> findByIdAndRecipientUserId(UUID id,UUID recipient);
    long countByRecipientUserIdAndReadAtIsNull(UUID recipient);
    List<SystemNotification> findByRecipientUserIdAndReadAtIsNull(UUID recipient);
}
