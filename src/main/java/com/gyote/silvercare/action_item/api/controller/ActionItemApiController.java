package com.gyote.silvercare.action_item.api.controller;

import com.gyote.silvercare.action_item.api.dto.request.ActionItemApproveRequestDto;
import com.gyote.silvercare.action_item.api.dto.response.ActionItemApprovalResponseDto;
import com.gyote.silvercare.action_item.api.dto.response.ActionItemPageResponseDto;
import com.gyote.silvercare.action_item.api.dto.response.ActionItemResponseDto;
import com.gyote.silvercare.action_item.api.mapper.ActionItemResponseMapper;
import com.gyote.silvercare.action_item.command.application.ActionItemCommandService;
import com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus;
import com.gyote.silvercare.action_item.query.application.ActionItemQueryService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.query.application.UserQueryService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 인증된 개인 또는 ACTIVE 보호자의 할 일 후보 조회 진입점이다. */
@RestController
@RequestMapping("/api/action-items")
public class ActionItemApiController {
    private final ActionItemQueryService queries;
    private final ActionItemResponseMapper mapper;
    private final UserQueryService users;
    private final ActionItemCommandService commands;

    public ActionItemApiController(
            ActionItemQueryService queries,
            ActionItemResponseMapper mapper,
            UserQueryService users,
            ActionItemCommandService commands) {
        this.queries = queries;
        this.mapper = mapper;
        this.users = users;
        this.commands = commands;
    }

    /** 대상 개인의 후보를 선택한 승인 상태와 커서 기준으로 조회한다. */
    @GetMapping
    public ActionItemPageResponseDto list(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) ActionItemApprovalStatus approvalStatus,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size) {
        return mapper.toResponse(
                queries.list(actor(principal), patientId, approvalStatus, cursor, size));
    }

    /** 현재 계정이 접근할 수 있는 후보의 상세와 원문 근거를 조회한다. */
    @GetMapping("/{actionItemId}")
    public ActionItemResponseDto detail(
            @AuthenticationPrincipal OAuth2User principal, @PathVariable UUID actionItemId) {
        return mapper.toResponse(queries.detail(actor(principal), actionItemId));
    }

    private User actor(OAuth2User principal) {
        Object id = principal.getAttributes().get("id");
        return users.requireByKakaoId(String.valueOf(id));
    }

    /** 대기 후보를 승인하고 원자적으로 생성한 일정 ID를 반환한다. */
    @PostMapping("/{actionItemId}/approve")
    public ActionItemApprovalResponseDto approve(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID actionItemId,
            @RequestBody ActionItemApproveRequestDto body) {
        var inputs =
                body.schedules() == null
                        ? null
                        : body.schedules().stream()
                                .map(row -> row == null ? null : row.toInput())
                                .toList();
        return new ActionItemApprovalResponseDto(
                actionItemId, commands.approve(actor(principal), actionItemId, inputs));
    }

    /** 대기 후보를 거절하고 변경된 상세를 반환한다. */
    @PostMapping("/{actionItemId}/reject")
    public ActionItemResponseDto reject(
            @AuthenticationPrincipal OAuth2User principal, @PathVariable UUID actionItemId) {
        User actor = actor(principal);
        commands.reject(actor, actionItemId);
        return mapper.toResponse(queries.detail(actor, actionItemId));
    }
}
