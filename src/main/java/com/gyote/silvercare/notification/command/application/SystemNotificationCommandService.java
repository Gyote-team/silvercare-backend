package com.gyote.silvercare.notification.command.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.notification.domain.entity.SystemNotification;
import com.gyote.silvercare.notification.domain.repository.SystemNotificationRepository;
import com.gyote.silvercare.notification.error.SystemNotificationErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 본인 시스템 알림의 읽음 상태를 변경한다. */
@Service
public class SystemNotificationCommandService {
    private final SystemNotificationRepository notifications;

    public SystemNotificationCommandService(SystemNotificationRepository notifications) {
        this.notifications = notifications;
    }

    /** 본인 알림만 읽음 처리한다. 다른 사람의 알림은 존재 여부를 숨긴다. */
    @Transactional
    public void read(UUID recipient, UUID id) {
        notifications
                .findByIdAndRecipientUserId(id, recipient)
                .orElseThrow(() -> new BusinessException(SystemNotificationErrorCode.NOT_FOUND))
                .markRead();
    }

    /** 본인 계정의 미읽음 알림을 모두 읽음 처리한다. */
    @Transactional
    public void readAll(UUID recipient) {
        notifications
                .findByRecipientUserIdAndReadAtIsNull(recipient)
                .forEach(SystemNotification::markRead);
    }
}
