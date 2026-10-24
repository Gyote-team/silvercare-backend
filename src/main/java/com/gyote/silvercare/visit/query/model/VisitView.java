package com.gyote.silvercare.visit.query.model;

import com.gyote.silvercare.visit.domain.VisitStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 병원 방문과 일반 조회 가능한 하위 자료 참조의 조회 모델. */
public record VisitView(
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
