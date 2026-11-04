package com.gyote.silvercare.previsit_summary.api.controller;

import com.gyote.silvercare.previsit_summary.api.dto.request.PrevisitSummaryRequestDto;
import com.gyote.silvercare.previsit_summary.api.dto.response.PrevisitSummaryResponseDto;
import com.gyote.silvercare.previsit_summary.api.mapper.PrevisitSummaryResponseMapper;
import com.gyote.silvercare.previsit_summary.command.application.PrevisitSummaryCommandService;
import com.gyote.silvercare.previsit_summary.query.application.PrevisitSummaryQueryService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.query.application.UserQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

/** 요약 요청·공통 작업 상태·결과 조회를 현재 계정과 연결한다. */
@RestController
public class PrevisitSummaryApiController {
    private final PrevisitSummaryCommandService commands;
    private final PrevisitSummaryQueryService queries;
    private final PrevisitSummaryResponseMapper mapper;
    private final UserQueryService users;
    public PrevisitSummaryApiController(PrevisitSummaryCommandService commands,PrevisitSummaryQueryService queries,
                                       PrevisitSummaryResponseMapper mapper,UserQueryService users) {
        this.commands=commands; this.queries=queries; this.mapper=mapper; this.users=users;
    }
    /** 요청은 즉시 202를 반환하고 작업 상태 경로를 안내한다. */
    @PostMapping("/api/previsit-summaries")
    public ResponseEntity<PrevisitSummaryResponseDto.Accepted> create(@AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody PrevisitSummaryRequestDto body,
            @RequestHeader(value="Idempotency-Key",required=false) String key) {
        UUID id=commands.create(actor(principal),body.patientId(),body.windowDays()==null?14:body.windowDays(),key);
        String url="/api/ai-operations/"+id;
        return ResponseEntity.accepted().location(URI.create(url)).body(new PrevisitSummaryResponseDto.Accepted(id,url));
    }
    /** 완료 전에는 결과 식별자를 반환하지 않는다. */
    @GetMapping("/api/ai-operations/{requestId}")
    public PrevisitSummaryResponseDto.Operation operation(@AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID requestId) { return mapper.operation(queries.operation(actor(principal),requestId)); }
    /** 저장 결과 ID로 현재 허용된 요약과 근거만 반환한다. */
    @GetMapping("/api/previsit-summaries/{summaryId}")
    public PrevisitSummaryResponseDto.Detail detail(@AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID summaryId) { return mapper.detail(queries.detail(actor(principal),summaryId)); }
    private User actor(OAuth2User principal) {
        Object id=principal.getAttribute("id"); return users.requireByKakaoId(String.valueOf(id));
    }
}
