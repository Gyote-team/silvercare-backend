package com.gyote.silvercare.voice_recording.api.dto.response;

import com.gyote.silvercare.global.status.TranscriptionStatus;

import java.time.Instant;
import java.util.UUID;

/** 비동기 상태 URL 및 사용자 확인용 초안 응답. */
public record VoiceTranscriptionResponseDto(
        UUID transcriptionId,
        UUID patientId,
        UUID visitId,
        String purpose,
        TranscriptionStatus status,
        String statusUrl,
        String draftText,
        String errorCode,
        UUID recordId,
        String confirmedText,
        long version,
        Instant createdAt) {}
