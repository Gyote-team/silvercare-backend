package com.gyote.silvercare.care_relation.query.model;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;

import java.util.UUID;

/** Query application layer에서 사용하는 읽기 전용 모델입니다. */
public record CareRelationView(
        UUID id,
        String counterpartName,
        String statusLabel,
        CareRelationStatus status,
        boolean canAccept,
        boolean canReject,
        boolean canCancel,
        boolean canRevoke
) {
}
