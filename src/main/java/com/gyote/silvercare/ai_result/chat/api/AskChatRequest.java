package com.gyote.silvercare.ai_result.chat.api;

import com.gyote.silvercare.ai_result.domain.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AskChatRequest(
        @NotBlank String question,
        @NotNull QuestionType questionType,
        @NotEmpty List<UUID> documentIds
) {
}
