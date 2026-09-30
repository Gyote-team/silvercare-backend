package com.gyote.silvercare.care_relation.domain;

public enum CareRelationStatus {
    /** 환자 또는 보호자가 연결을 요청하고 상대방의 응답을 기다리는 상태입니다. */
    REQUESTED,
    /** 환자와 보호자의 돌봄 관계가 승인되어 활성화된 상태입니다. */
    ACTIVE,
    /** 연결 요청이 상대방에 의해 거절된 상태입니다. */
    REJECTED,
    /** 요청자 또는 관계 당사자가 연결 요청을 취소한 상태입니다. */
    CANCELED,
    /** 기존에 활성화된 돌봄 관계가 해제된 상태입니다. */
    REVOKED
}
