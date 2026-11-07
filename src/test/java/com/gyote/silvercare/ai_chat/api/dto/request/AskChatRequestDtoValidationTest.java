package com.gyote.silvercare.ai_chat.api.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AskChatRequestDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNullDocumentIdElement() {
        AskChatRequestDto request = new AskChatRequestDto(
                "검사 결과를 알려줘",
                null,
                Arrays.asList(UUID.randomUUID(), null)
        );

        assertThat(validator.validate(request))
                .anySatisfy(violation ->
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("documentIds[1].<list element>"));
    }
}
