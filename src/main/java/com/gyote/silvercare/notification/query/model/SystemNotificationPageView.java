package com.gyote.silvercare.notification.query.model;
import java.time.Instant;
import java.util.*;
public record SystemNotificationPageView(List<Item> items,long unreadCount) {
    public record Item(UUID id,String type,String message,String path,Instant createdAt,Instant readAt) {}
}
