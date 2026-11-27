package com.gyote.silvercare.action_item.api.dto.response;

import com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus;
import com.gyote.silvercare.action_item.domain.ActionItemType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 후보 조회 공개 응답이며 저장되지 않은 근거와 신뢰도는 null로 반환한다. */
public record ActionItemResponseDto(
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
        String sourceExcerpt,
        UUID approvedByUserId,
        Instant approvedAt,
        Instant createdAt) {

    /** 공개하는 원문 근거의 페이지와 발췌문이다. 페이지 미상은 null이다. */
    public record Source(Integer pageNo, String sourceText) {}
}
