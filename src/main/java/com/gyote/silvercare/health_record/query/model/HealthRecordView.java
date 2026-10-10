package com.gyote.silvercare.health_record.query.model;

import java.time.Instant;
import java.util.UUID;

/** 건강기록 상세와 작성자 정보를 표현하는 조회 모델. */
public record HealthRecordView(
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
