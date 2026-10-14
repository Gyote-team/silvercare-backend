package com.gyote.silvercare.ai_result.chat.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateChatSessionRequest(@NotNull UUID patientId, String title) {
}
