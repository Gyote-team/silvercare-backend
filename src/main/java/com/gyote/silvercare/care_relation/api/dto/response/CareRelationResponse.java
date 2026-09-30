package com.gyote.silvercare.care_relation.api.dto.response;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;

import java.time.Instant;
import java.util.UUID;

/** acceptedAt은 연결이 수락된 시각이며, 수락 전이면 null이다. */
public record CareRelationResponse(
        UUID id,
        UUID patientId,
        String counterpartName,
        String statusLabel,
        CareRelationStatus status,
        boolean canAccept,
        boolean canReject,
        boolean canCancel,
        boolean canRevoke,
        Instant acceptedAt
) {
}
