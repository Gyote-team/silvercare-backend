package com.gyote.silvercare.ai_result.chat.api;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionResponse(UUID sessionId, UUID patientId, String title, Instant createdAt) {
}
