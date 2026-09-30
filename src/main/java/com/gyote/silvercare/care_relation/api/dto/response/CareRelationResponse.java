package com.gyote.silvercare.care_relation.api.dto.response;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * acceptedAt은 수락 전이면 null이다.
 * endedAt은 거절·취소·해제된 시각이며, 대기 중이거나 연결 중이면 null이다.
 */
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
        Instant requestedAt,
        Instant acceptedAt,
        Instant endedAt
) {
}
