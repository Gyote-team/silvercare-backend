package com.gyote.silvercare.notification.domain.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="system_notifications") @Getter @NoArgsConstructor
public class SystemNotification {
    @Id private UUID id;
    @Column(name="recipient_user_id",nullable=false) private UUID recipientUserId;
    @Column(nullable=false) private String type;
    @Column(nullable=false) private String message;
    @Column(nullable=false) private String path;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="read_at") private Instant readAt;
    public SystemNotification(UUID recipient,String type,String message,String path) {
        id=UUID.randomUUID();recipientUserId=recipient;this.type=type;this.message=message;this.path=path;createdAt=Instant.now();
    }
    /** 자신의 알림을 읽음 처리한다. 재요청도 같은 결과를 유지한다. */
    public void markRead() { if(readAt==null) readAt=Instant.now(); }
}
