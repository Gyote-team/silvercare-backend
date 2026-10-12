package com.gyote.silvercare.notification.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 시스템 알림 목록과 미읽음 수를 반환하는 HTTP 응답 계약. */
public record SystemNotificationPageResponseDto(List<Item> items, long unreadCount) {
    /** 개별 알림의 표시 정보와 읽음 상태. */
    public record Item(
            UUID id, String type, String message, String path, Instant createdAt, Instant readAt) {}
}
