package com.gyote.silvercare.ai_result.chat.api;

import java.util.UUID;

public record CreateChatSessionRequest(UUID patientId, String title) {
}
