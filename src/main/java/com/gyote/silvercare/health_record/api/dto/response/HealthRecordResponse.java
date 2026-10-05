package com.gyote.silvercare.health_record.api.dto.response;

import com.gyote.silvercare.health_record.domain.HealthRecordInputType;

import java.time.Instant;
import java.util.UUID;

public record HealthRecordResponse(
        UUID id,
        UUID patientId,
        UUID visitId,
        String authorName,
        UUID authorUserId,
        HealthRecordInputType inputType,
        String content,
        Instant recordedAt,
        boolean proxyWritten,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
