package com.gyote.silvercare.ai_result.chat.api;

import com.gyote.silvercare.ai_result.chat.ChatRole;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(UUID messageId, ChatRole role, String content, Instant createdAt, UUID resultDocumentId) {
}
