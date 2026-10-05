package com.gyote.silvercare.care_relation.query.model;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.user.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

/** Query application layer에서 사용하는 읽기 전용 모델입니다. */
public record CareRelationView(
        UUID id,
        UUID patientId,
        UUID caregiverId,
        String counterpartName,
        UserRole counterpartRole,
        String statusLabel,
        CareRelationStatus status,
        boolean canAccept,
        boolean canReject,
        boolean canCancel,
        boolean canRevoke,
        Instant requestedAt,
        Instant acceptedAt,
        Instant endedAt
) {
}
