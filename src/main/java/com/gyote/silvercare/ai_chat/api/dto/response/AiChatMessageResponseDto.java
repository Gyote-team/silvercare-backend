package com.gyote.silvercare.ai_chat.api.dto.response;

import com.gyote.silvercare.ai_chat.domain.ChatRole;

import java.time.Instant;
import java.util.UUID;

public record AiChatMessageResponseDto(
        UUID messageId,
        ChatRole role,
        String content,
        Instant createdAt,
        UUID resultDocumentId
) {
}
