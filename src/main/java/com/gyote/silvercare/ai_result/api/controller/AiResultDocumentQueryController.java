package com.gyote.silvercare.ai_result.api.controller;

import com.gyote.silvercare.ai_result.api.dto.response.AiResultDocumentResponseDto;
import com.gyote.silvercare.ai_result.api.mapper.AiResultDocumentResponseMapper;
import com.gyote.silvercare.ai_result.query.application.AiResultDocumentQueryService;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.error.UserErrorCode;
import com.gyote.silvercare.user.query.application.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;
import com.gyote.silvercare.ai_result.command.application.AiDetailedDocumentService;

/** 저장된 AI 답변의 원문 근거 조회 API를 제공한다. */
@RestController
@RequestMapping("/api/ai-result-documents")
@RequiredArgsConstructor
public class AiResultDocumentQueryController {

    private final AiResultDocumentQueryService queries;
    private final AiResultDocumentResponseMapper mapper;
    private final UserQueryService users;
    private final AiDetailedDocumentService detailedDocuments;

    /** 생성된 AI 답변 문서를 식별자로 조회한다. */
    @GetMapping("/{resultDocumentId}")
    public AiResultDocumentResponseDto get(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID resultDocumentId
    ) {
        return mapper.toResponse(queries.get(actor(principal), resultDocumentId));
    }

    @GetMapping
    public List<AiResultDocumentResponseDto> list(@AuthenticationPrincipal OAuth2User principal) {
        return queries.listSaved(actor(principal)).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{resultDocumentId}/save")
    public AiResultDocumentResponseDto save(@AuthenticationPrincipal OAuth2User principal,
                                             @PathVariable UUID resultDocumentId) {
        User actor = actor(principal);
        queries.save(actor, resultDocumentId);
        detailedDocuments.create(actor, resultDocumentId);
        return mapper.toResponse(queries.get(actor, resultDocumentId));
    }

    /** 인증 정보에서 현재 요청 사용자를 조회한다. */
    private User actor(OAuth2User principal) {
        Object userId = principal.getAttributes().get("userId");
        if (userId == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        return users.requireById(UUID.fromString(String.valueOf(userId)));
    }
}
