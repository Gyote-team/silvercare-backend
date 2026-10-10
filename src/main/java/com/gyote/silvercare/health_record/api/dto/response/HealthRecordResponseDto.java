package com.gyote.silvercare.health_record.api.dto.response;
import java.time.Instant;
import java.util.UUID;
public record HealthRecordResponseDto(UUID recordId, UUID patientId, UUID authorUserId, String authorName,
        UUID visitId, String body, Instant recordedAt, Instant createdAt, Instant updatedAt, boolean proxyWritten) {}
