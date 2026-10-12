package com.gyote.silvercare.notification.api.mapper;

import com.gyote.silvercare.notification.api.dto.response.SystemNotificationPageResponseDto;
import com.gyote.silvercare.notification.query.model.SystemNotificationPageView;

import org.springframework.stereotype.Component;

/** 시스템 알림 조회 모델을 HTTP 응답 DTO로 변환한다. */
@Component
public class SystemNotificationResponseMapper {
    /** 조회 목록과 미읽음 수를 기존 HTTP 필드 계약으로 반환한다. */
    public SystemNotificationPageResponseDto toResponse(SystemNotificationPageView view) {
        return new SystemNotificationPageResponseDto(
                view.items().stream()
                        .map(
                                item ->
                                        new SystemNotificationPageResponseDto.Item(
                                                item.id(),
                                                item.type(),
                                                item.message(),
                                                item.path(),
                                                item.createdAt(),
                                                item.readAt()))
                        .toList(),
                view.unreadCount());
    }
}
