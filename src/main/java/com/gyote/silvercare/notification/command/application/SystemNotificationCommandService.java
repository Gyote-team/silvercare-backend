package com.gyote.silvercare.notification.command.application;
import com.gyote.silvercare.notification.domain.entity.SystemNotification;
import com.gyote.silvercare.notification.domain.repository.SystemNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@Service
public class SystemNotificationCommandService {
    private final SystemNotificationRepository notifications;
    public SystemNotificationCommandService(SystemNotificationRepository notifications) {this.notifications=notifications;}
    /** 본인 알림만 읽음 처리한다. 다른 사람의 알림은 존재 여부를 숨긴다. */
    @Transactional
    public void read(UUID recipient,UUID id) {
        notifications.findByIdAndRecipientUserId(id,recipient)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)).markRead();
    }
    /** 본인 계정의 미읽음 알림을 모두 읽음 처리한다. */
    @Transactional
    public void readAll(UUID recipient) {
        notifications.findByRecipientUserIdAndReadAtIsNull(recipient).forEach(SystemNotification::markRead);
    }
}
