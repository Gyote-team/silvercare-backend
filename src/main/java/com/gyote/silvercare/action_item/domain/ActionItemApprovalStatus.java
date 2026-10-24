package com.gyote.silvercare.action_item.domain;

/** 할 일 후보의 승인 상태입니다. */
public enum ActionItemApprovalStatus {
    /** 사용자의 승인·거절을 기다리는 상태입니다. */
    PENDING,
    /** 사용자가 승인한 상태입니다. */
    APPROVED,
    /** 사용자가 거절한 상태입니다. */
    REJECTED,
    /** 원본 문서 삭제 등으로 취소된 상태입니다. */
    CANCELED
}
