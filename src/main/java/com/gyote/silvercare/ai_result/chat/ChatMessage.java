package com.gyote.silvercare.ai_result.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {
    @Id
    private UUID id;
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(name = "result_document_id")
    private UUID resultDocumentId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // 지정한 역할과 내용으로 새 채팅 메시지를 생성한다.
    public static ChatMessage of(UUID sessionId, ChatRole role, String content) {
        ChatMessage message = new ChatMessage();
        message.id = UUID.randomUUID();
        message.sessionId = sessionId;
        message.role = role;
        message.content = content;
        message.createdAt = Instant.now();
        return message;
    }

    // AI 답변 문서와 연결된 어시스턴트 메시지를 생성한다.
    public static ChatMessage answer(UUID sessionId, String content, UUID resultDocumentId) {
        ChatMessage message = of(sessionId, ChatRole.ASSISTANT, content);
        message.resultDocumentId = resultDocumentId;
        return message;
    }
}
