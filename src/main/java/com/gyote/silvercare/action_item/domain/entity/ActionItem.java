package com.gyote.silvercare.action_item.domain.entity;

import com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus;
import com.gyote.silvercare.action_item.domain.ActionItemType;
import com.gyote.silvercare.action_item.error.ActionItemErrorCode;
import com.gyote.silvercare.global.exception.BusinessException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 진료 문서에서 나온 할 일 후보와 승인 상태를 관리하는 엔티티입니다. */
@Entity
@Table(name = "action_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionItem {

    @Id private UUID id;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionItemType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_at")
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ActionItemApprovalStatus approvalStatus;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "cancel_reason", length = 100)
    private String cancelReason;

    @Column(name = "source_item_id")
    private UUID sourceItemId;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    /** 대기 후보만 승인하고 실제 승인자와 시각을 기록한다. */
    public void approve(UUID actorId) {
        decide(ActionItemApprovalStatus.APPROVED, actorId);
    }

    /** 대기 후보를 거절하고 공통 처리자·처리 시각 필드에 거절 이력을 기록한다. */
    public void reject(UUID actorId) {
        decide(ActionItemApprovalStatus.REJECTED, actorId);
    }

    private void decide(ActionItemApprovalStatus status, UUID actorId) {
        requirePending();
        if (actorId == null)
            throw new BusinessException(
                    com.gyote.silvercare.global.exception.GlobalErrorCode.INVALID_REQUEST);
        approvalStatus = status;
        // 기존 협업 계약의 필드명은 승인·거절 모두 동일하게 사용한다.
        approvedByUserId = actorId;
        approvedAt = Instant.now();
    }

    /** 승인·거절·원본 삭제로 종료된 후보는 다시 처리할 수 없다. */
    public void requirePending() {
        if (approvalStatus != ActionItemApprovalStatus.PENDING)
            throw new BusinessException(ActionItemErrorCode.STATE_CONFLICT);
    }
}
