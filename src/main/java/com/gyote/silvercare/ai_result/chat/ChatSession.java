package com.gyote.silvercare.ai_result.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {
    @Id
    private UUID id;
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "started_by_user_id", nullable = false)
    private UUID startedByUserId;
    private String title;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static ChatSession open(UUID patientId, UUID userId, String title) {
        ChatSession session = new ChatSession();
        session.id = UUID.randomUUID();
        session.patientId = patientId;
        session.startedByUserId = userId;
        session.title = title;
        session.createdAt = Instant.now();
        return session;
    }
}
