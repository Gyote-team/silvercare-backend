package com.gyote.silvercare.visit.api.controller;

import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.query.application.UserQueryService;
import com.gyote.silvercare.visit.api.dto.request.VisitWriteRequestDto;
import com.gyote.silvercare.visit.api.dto.response.VisitPageResponseDto;
import com.gyote.silvercare.visit.api.dto.response.VisitResponseDto;
import com.gyote.silvercare.visit.api.mapper.VisitResponseMapper;
import com.gyote.silvercare.visit.command.application.VisitCommandService;
import com.gyote.silvercare.visit.query.application.VisitQueryService;

import jakarta.validation.Valid;

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

/** 개인 및 ACTIVE 보호자의 병원 방문 관리 HTTP 진입점. */
@RestController
@RequestMapping("/api/visits")
public class VisitApiController {
    private final VisitCommandService commands;
    private final VisitQueryService queries;
    private final VisitResponseMapper mapper;
    private final UserQueryService users;

    public VisitApiController(
            VisitCommandService commands,
            VisitQueryService queries,
            VisitResponseMapper mapper,
            UserQueryService users) {
        this.commands = commands;
        this.queries = queries;
        this.mapper = mapper;
        this.users = users;
    }

    /** 현재 역할이 접근 가능한 방문 목록을 조회한다. */
    @GetMapping
    public VisitPageResponseDto list(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "false") boolean includeCanceled) {
        return mapper.toResponse(
                queries.list(actor(principal), patientId, cursor, size, includeCanceled));
    }

    /** 취소 여부와 관계없이 당사자의 방문 상세를 조회한다. */
    @GetMapping("/{visitId}")
    public VisitResponseDto detail(
            @AuthenticationPrincipal OAuth2User principal, @PathVariable UUID visitId) {
        return mapper.toResponse(queries.detail(actor(principal), visitId));
    }

    /** 대상 개인에 방문을 등록한다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitResponseDto create(
            @AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody VisitWriteRequestDto body) {
        User actor = actor(principal);
        var visit =
                commands.create(
                        actor,
                        body.patientId(),
                        body.visitedOn(),
                        body.hospitalName(),
                        body.department(),
                        body.memo(),
                        body.visitStatus());
        return mapper.toResponse(queries.detail(actor, visit.getId()));
    }

    /** 원래 개인과 참조를 보존하며 방문 정보를 수정한다. */
    @PutMapping("/{visitId}")
    public VisitResponseDto update(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID visitId,
            @Valid @RequestBody VisitWriteRequestDto body) {
        User actor = actor(principal);
        var visit =
                commands.update(
                        actor,
                        visitId,
                        body.patientId(),
                        body.visitedOn(),
                        body.hospitalName(),
                        body.department(),
                        body.memo(),
                        body.visitStatus());
        return mapper.toResponse(queries.detail(actor, visit.getId()));
    }

    /** 방문을 취소한다. 반복 요청에도 하위 자료를 삭제하지 않는다. */
    @DeleteMapping("/{visitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID visitId) {
        commands.cancel(actor(principal), visitId);
    }

    private User actor(OAuth2User principal) {
        Object key = principal.getAttribute("id");
        return users.requireByKakaoId(String.valueOf(key));
    }
}
