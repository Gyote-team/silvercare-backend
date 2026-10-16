package com.gyote.silvercare.ai_chat.api.dto.request;

import com.gyote.silvercare.ai_result.domain.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AskChatRequestDto(
        @NotBlank String question,
        @NotNull QuestionType questionType,
        @NotEmpty List<UUID> documentIds
) {
}
