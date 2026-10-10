package com.gyote.silvercare.health_record.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/** 건강기록 및 원래 작성자 정보를 반환하는 HTTP 응답. */
public record HealthRecordResponseDto(
        UUID recordId,
        UUID patientId,
        UUID authorUserId,
        String authorName,
        UUID visitId,
        String body,
        Instant recordedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean proxyWritten) {}
