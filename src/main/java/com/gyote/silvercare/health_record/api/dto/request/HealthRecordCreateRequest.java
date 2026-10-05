package com.gyote.silvercare.health_record.api.dto.request;

import com.gyote.silvercare.health_record.domain.HealthRecordInputType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record HealthRecordCreateRequest(
        UUID patientId,
        UUID visitId,
        @NotNull HealthRecordInputType inputType,
        @NotBlank @Size(max = 5000) String content,
        Instant recordedAt,
        @Size(max = 64) String idempotencyKey
) {
}
