package com.gyote.silvercare.medical_document.api.controller;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentCitationsResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentFactsResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentPageResponseDto;
import com.gyote.silvercare.medical_document.error.AiDocumentErrorCode;
import com.gyote.silvercare.medical_document.query.application.AiDocumentEvidenceQueryService;
import com.gyote.silvercare.medical_document.query.application.AiDocumentQueryService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.error.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** AI 설명의 값 검증 및 원문 근거 API입니다. */
@RestController
@RequiredArgsConstructor
public class AiDocumentEvidenceController {

    private final AiDocumentEvidenceQueryService evidence;
    private final AiDocumentQueryService queries;

    /** AI 설명에 사용된 값과 원문 추출값의 대조 결과를 반환합니다. */
    @GetMapping("/api/ai-documents/{documentId}/facts")
    public AiDocumentFactsResponseDto facts(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable String documentId
    ) {
        User actor = queries.requireUser(userId(principal));
        return evidence.facts(actor, uuid(documentId));
    }

    /** AI 설명 항목이 참조한 유효한 원문 인용 목록을 반환합니다. */
    @GetMapping("/api/ai-documents/{documentId}/citations")
    public AiDocumentCitationsResponseDto citations(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable String documentId,
            @RequestParam(required = false) String sectionId
    ) {
        User actor = queries.requireUser(userId(principal));
        return evidence.citations(actor, uuid(documentId), optionalUuid(sectionId));
    }

    /** 페이지 렌더링 이미지가 있으면 이를, 없으면 원본 문서 URL을 반환합니다. */
    @GetMapping("/api/ai-documents/{documentId}/pages/{pageNo}")
    public AiDocumentPageResponseDto page(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable String documentId,
            @PathVariable int pageNo,
            @RequestParam(required = false) String anchorId
    ) {
        User actor = queries.requireUser(userId(principal));
        return evidence.page(actor, uuid(documentId), pageNo, anchorId);
    }

    /** 경로 식별자의 UUID 형식을 검증합니다. */
    private static UUID uuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(AiDocumentErrorCode.INVALID_FILTER);
        }
    }

    private static UUID optionalUuid(String value) {
        return value == null || value.isBlank() ? null : uuid(value);
    }

    /** OAuth 속성에서 내부 사용자 식별자를 추출합니다. */
    private static UUID userId(OAuth2User user) {
        Object value = user.getAttributes().get("userId");
        if (value == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
    }
}
