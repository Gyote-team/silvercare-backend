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

    @PostMapping("/api/ai-result-documents")
    public AiResultDocumentResponse create(
            @AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody CreateAiResultDocumentRequest request
    ) {
        return service.create(actor(principal), request);
    }

    @GetMapping("/api/ai-result-documents/{resultDocumentId}")
    public AiResultDocumentResponse get(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID resultDocumentId
    ) {
        return service.get(actor(principal), resultDocumentId);
    }

    @GetMapping("/api/ai-operations/{requestId}")
    public AiResultDocumentResponse status(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID requestId
    ) {
        return service.getByRequestId(actor(principal), requestId);
    }

    private User actor(OAuth2User principal) {
        Object userId = principal.getAttributes().get("userId");
        if (userId == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        return users.requireById(UUID.fromString(String.valueOf(userId)));
    }
}
