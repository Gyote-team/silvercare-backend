package com.gyote.silvercare.care_relation.api.dto.response;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.user.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * patientId는 개인 건강 프로필(patients)의 ID이고, caregiverUserId는 보호자 계정(users)의 ID다.
 * acceptedAt은 수락 전이면 null이다.
 * endedAt은 거절·취소·해제된 시각이며, 대기 중이거나 연결 중이면 null이다.
 */
public record CareRelationResponse(
        UUID id,
        UUID patientId,
        UUID caregiverUserId,
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
