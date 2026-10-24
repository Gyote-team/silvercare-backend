package com.gyote.silvercare.notification.query.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 시스템 알림 목록과 전체 미읽음 수를 표현하는 조회 모델. */
public record SystemNotificationPageView(List<Item> items, long unreadCount) {
    /** 개별 시스템 알림의 표시 정보 및 읽음 상태. */
    public record Item(
            UUID id, String type, String message, String path, Instant createdAt, Instant readAt) {}
}
