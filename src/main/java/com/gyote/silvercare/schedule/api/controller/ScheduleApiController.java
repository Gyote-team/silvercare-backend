package com.gyote.silvercare.schedule.api.controller;

import com.gyote.silvercare.schedule.api.dto.request.ScheduleWriteRequestDto;
import com.gyote.silvercare.schedule.api.dto.response.SchedulePageResponseDto;
import com.gyote.silvercare.schedule.api.dto.response.ScheduleResponseDto;
import com.gyote.silvercare.schedule.api.mapper.ScheduleResponseMapper;
import com.gyote.silvercare.schedule.command.application.ScheduleCommandService;
import com.gyote.silvercare.schedule.query.application.ScheduleQueryService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.query.application.UserQueryService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleApiController {
    private final ScheduleCommandService commands;
    private final ScheduleQueryService queries;
    private final ScheduleResponseMapper mapper;
    private final UserQueryService users;

    public ScheduleApiController(
            ScheduleCommandService c,
            ScheduleQueryService q,
            ScheduleResponseMapper m,
            UserQueryService u) {
        commands = c;
        queries = q;
        mapper = m;
        users = u;
    }

    /** 현재 접근 가능한 개인의 일정 규칙 목록을 조회한다. */
    @GetMapping
    public SchedulePageResponseDto list(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return mapper.toResponse(queries.list(actor(principal), patientId, page, size));
    }

    /** 현재 일정과 반복 규칙·수정 버전을 조회한다. */
    @GetMapping("/{id}")
    public ScheduleResponseDto detail(
            @AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id) {
        return mapper.toResponse(queries.detail(actor(principal), id));
    }

    /** 후보와 독립된 수동 일정을 등록한다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponseDto create(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody ScheduleWriteRequestDto body) {
        User actor = actor(principal);
        UUID id = commands.create(actor, body.patientId(), body.toInput());
        return mapper.toResponse(queries.detail(actor, id));
    }

    /** 일정 소유자와 원본 후보를 보존하며 반복 전체를 수정한다. */
    @PutMapping("/{id}")
    public ScheduleResponseDto update(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id,
            @RequestBody ScheduleWriteRequestDto body) {
        User actor = actor(principal);
        commands.update(actor, id, body.patientId(), body.toInput(), body.status(), body.version());
        return mapper.toResponse(queries.detail(actor, id));
    }

    /** 버전이 일치하는 반복 전체를 소프트 삭제한다. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id,
            @RequestParam(required = false) Long version) {
        commands.delete(actor(principal), id, version);
    }

    private User actor(OAuth2User principal) {
        Object key = principal.getAttribute("id");
        return users.requireByKakaoId(String.valueOf(key));
    }
}
