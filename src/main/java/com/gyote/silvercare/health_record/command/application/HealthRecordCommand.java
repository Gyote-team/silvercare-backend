package com.gyote.silvercare.health_record.command.application;

import com.gyote.silvercare.health_record.domain.HealthRecordInputType;

import java.time.Instant;
import java.util.UUID;

public record HealthRecordCommand(
        UUID patientId,
        UUID visitId,
        HealthRecordInputType inputType,
        String content,
        Instant recordedAt,
        String idempotencyKey
) {
}
