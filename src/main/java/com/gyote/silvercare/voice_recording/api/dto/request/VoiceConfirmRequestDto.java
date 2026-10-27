package com.gyote.silvercare.voice_recording.api.dto.request;

import jakarta.validation.constraints.*;

/** 사용자 수정·확인한 본문과 조회 시점 버전. */
public record VoiceConfirmRequestDto(
        @NotBlank @Size(max = 2000) String body, @NotNull @PositiveOrZero Long version) {}
