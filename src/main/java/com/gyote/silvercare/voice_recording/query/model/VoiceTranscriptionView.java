package com.gyote.silvercare.voice_recording.query.model;

import com.gyote.silvercare.global.status.TranscriptionStatus;

import java.time.Instant;
import java.util.UUID;

/** 저장 키나 원본 URL을 노출하지 않는 초안 조회 모델. */
public record VoiceTranscriptionView(
        UUID transcriptionId,
        UUID patientId,
        UUID visitId,
        String purpose,
        TranscriptionStatus status,
        String draftText,
        String errorCode,
        UUID recordId,
        String confirmedText,
        long version,
        Instant createdAt) {}
