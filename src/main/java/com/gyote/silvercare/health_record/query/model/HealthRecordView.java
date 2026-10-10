package com.gyote.silvercare.health_record.query.model;
import java.time.Instant;
import java.util.UUID;
public record HealthRecordView(UUID recordId, UUID patientId, UUID authorUserId, String authorName,
        UUID visitId, String body, Instant recordedAt, Instant createdAt, Instant updatedAt, boolean proxyWritten) {}
