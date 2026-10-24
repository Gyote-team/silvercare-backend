package com.gyote.silvercare.ai_chat.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AiChatSessionResponseDto(UUID sessionId, UUID patientId, String title, Instant createdAt) {
}
