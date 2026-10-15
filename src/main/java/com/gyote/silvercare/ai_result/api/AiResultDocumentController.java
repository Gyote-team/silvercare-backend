package com.gyote.silvercare.ai_result.api;

import com.gyote.silvercare.ai_result.api.dto.AiResultDocumentResponse;
import com.gyote.silvercare.ai_result.api.dto.CreateAiResultDocumentRequest;
import com.gyote.silvercare.ai_result.application.AiResultDocumentService;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.error.UserErrorCode;
import com.gyote.silvercare.user.query.application.UserQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AiResultDocumentController {

    private final AiResultDocumentService service;
    private final UserQueryService users;

    // 선택한 문서 근거로 AI 답변 생성을 요청한다.
    @PostMapping("/api/ai-result-documents")
    public AiResultDocumentResponse create(
            @AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody CreateAiResultDocumentRequest request
    ) {
        return service.create(actor(principal), request);
    }

    // 생성된 AI 답변 문서를 식별자로 조회한다.
    @GetMapping("/api/ai-result-documents/{resultDocumentId}")
    public AiResultDocumentResponse get(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID resultDocumentId
    ) {
        return service.get(actor(principal), resultDocumentId);
    }

    // AI 답변 생성 요청의 처리 상태를 조회한다.
    @GetMapping("/api/ai-operations/{requestId}")
    public AiResultDocumentResponse status(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID requestId
    ) {
        return service.getByRequestId(actor(principal), requestId);
    }

    // 인증 정보에서 현재 요청 사용자를 조회한다.
    private User actor(OAuth2User principal) {
        Object userId = principal.getAttributes().get("userId");
        if (userId == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        return users.requireById(UUID.fromString(String.valueOf(userId)));
    }
}
