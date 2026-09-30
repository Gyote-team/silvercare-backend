package com.gyote.silvercare.care_relation.api.dto.response;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.user.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public record CareRelationResponse(
        UUID id,
        UUID relationId,
        UUID patientId,
        String counterpartName,
        UserRole counterpartRole,
        String statusLabel,
        CareRelationStatus status,
        Instant requestedAt,
        Instant acceptedAt,
        Instant revokedAt,
        boolean canAccept,
        boolean canReject,
        boolean canCancel,
        boolean canRevoke
) {
}
