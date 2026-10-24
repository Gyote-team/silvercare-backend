package com.gyote.silvercare.notification.domain;

/** 연결 관계 및 건강기록에서 발생하는 시스템 알림 종류. */
public enum SystemActivityType {
    RELATION_REQUESTED,
    RELATION_ACCEPTED,
    RELATION_REJECTED,
    RELATION_CANCELED,
    RELATION_REVOKED,
    HEALTH_RECORD_CREATED
}
