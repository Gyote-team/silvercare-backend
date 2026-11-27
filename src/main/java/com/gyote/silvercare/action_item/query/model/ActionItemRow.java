package com.gyote.silvercare.action_item.query.model;

import com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus;
import com.gyote.silvercare.action_item.domain.ActionItemType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 소유권과 출처 일치 여부를 검증해 조합한 후보 조회 결과다. */
public record ActionItemRow(
        UUID actionItemId,
        UUID patientId,
        UUID visitId,
        UUID documentId,
        String documentName,
        ActionItemType type,
        String title,
        String description,
        Instant estimatedAt,
        ActionItemApprovalStatus approvalStatus,
        String cancelReason,
        boolean sourceDocumentDeleted,
        UUID sourceItemId,
        BigDecimal confidence,
        Source source,
        UUID approvedByUserId,
        Instant approvedAt,
        Instant createdAt) {

    /** 같은 출처 문서에 귀속된 실제 추출 항목의 원문 위치와 발췌문이다. */
    public record Source(Integer pageNo, String sourceText) {}
}
