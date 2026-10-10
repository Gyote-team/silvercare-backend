package com.gyote.silvercare.notification.api.controller;

import com.gyote.silvercare.notification.command.application.SystemNotificationCommandService;
import com.gyote.silvercare.notification.query.application.SystemNotificationQueryService;
import com.gyote.silvercare.notification.query.model.SystemNotificationPageView;
import com.gyote.silvercare.user.query.application.UserQueryService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 현재 역할 계정의 시스템 알림 조회와 읽음 요청을 처리한다. */
@RestController
@RequestMapping("/api/notifications")
public class SystemNotificationApiController {
    private final SystemNotificationQueryService queries;
    private final SystemNotificationCommandService commands;
    private final UserQueryService users;

    public SystemNotificationApiController(
            SystemNotificationQueryService queries,
            SystemNotificationCommandService commands,
            UserQueryService users) {
        this.queries = queries;
        this.commands = commands;
        this.users = users;
    }

    /** 현재 역할 계정의 알림을 조회한다. */
    @GetMapping
    public SystemNotificationPageView list(@AuthenticationPrincipal OAuth2User principal) {
        return queries.list(actorId(principal));
    }

    /** 현재 계정의 알림을 읽음 처리한다. */
    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        commands.read(actorId(principal), id);
    }

    /** 현재 계정의 알림을 모두 읽음 처리한다. */
    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal OAuth2User principal) {
        commands.readAll(actorId(principal));
    }

    private UUID actorId(OAuth2User principal) {
        Object key = principal.getAttribute("id");
        return users.requireByKakaoId(String.valueOf(key)).getId();
    }
}
