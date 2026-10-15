package com.gyote.silvercare.visit.api.dto.response;

import com.gyote.silvercare.visit.domain.VisitStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 방문 상세 및 연결 자료 ID의 HTTP 응답. */
public record VisitResponseDto(
        UUID visitId,
        UUID patientId,
        LocalDate visitedOn,
        String hospitalName,
        String department,
        String memo,
        VisitStatus visitStatus,
        List<UUID> documentIds,
        List<UUID> healthRecordIds,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
