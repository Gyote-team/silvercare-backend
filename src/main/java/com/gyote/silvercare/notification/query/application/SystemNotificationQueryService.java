package com.gyote.silvercare.notification.query.application;
import com.gyote.silvercare.notification.domain.repository.SystemNotificationRepository;
import com.gyote.silvercare.notification.query.model.SystemNotificationPageView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;
@Service
public class SystemNotificationQueryService {
    private final SystemNotificationRepository notifications;
    public SystemNotificationQueryService(SystemNotificationRepository notifications) { this.notifications=notifications; }
    /** 현재 계정의 최근 알림과 전체 미읽음 수를 반환한다. */
    @Transactional(readOnly=true)
    public SystemNotificationPageView list(UUID recipient) {
        return new SystemNotificationPageView(notifications.findByRecipientUserIdOrderByCreatedAtDescIdDesc(recipient,PageRequest.of(0,50))
            .stream().map(n->new SystemNotificationPageView.Item(n.getId(),n.getType(),n.getMessage(),n.getPath(),n.getCreatedAt(),n.getReadAt())).toList(),
            notifications.countByRecipientUserIdAndReadAtIsNull(recipient));
    }
}
