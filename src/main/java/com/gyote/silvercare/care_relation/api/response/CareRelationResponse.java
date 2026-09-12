package com.gyote.silvercare.care_relation.api.response;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;

import java.util.UUID;

public record CareRelationResponse(
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
