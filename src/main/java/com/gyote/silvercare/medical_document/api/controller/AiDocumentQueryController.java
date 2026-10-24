package com.gyote.silvercare.medical_document.api.controller;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.api.dto.request.AiDocumentListRequestDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentDetailResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentExplanationStatusResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentListResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.AiDocumentSectionsResponseDto;
import com.gyote.silvercare.medical_document.api.mapper.AiDocumentResponseMapper;
import com.gyote.silvercare.medical_document.query.application.AiDocumentQueryService;
import com.gyote.silvercare.medical_document.query.model.AiDocumentListView;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.error.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AiDocumentQueryController {

    private final AiDocumentQueryService queries;
    private final AiDocumentResponseMapper responseMapper;

    /** AI 설명 조회 대상 문서 목록을 반환한다. */
    @GetMapping("/api/ai-documents")
    public AiDocumentListResponseDto list(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String visitId,
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User actor = queries.requireUser(userId(principal));
        Page<AiDocumentListView> result = queries.listDocuments(
                actor,
                new AiDocumentListRequestDto(patientId, visitId, docType, status, page, size)
        );
        return responseMapper.toListResponse(result);
    }

    /** 특정 문서의 AI 설명 상세 정보를 반환한다. */
    @GetMapping("/api/ai-documents/{documentId}")
    public AiDocumentDetailResponseDto getDocument(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable String documentId
    ) {
        User actor = queries.requireUser(userId(principal));
        return responseMapper.toDetailResponse(queries.getDocument(actor, parseUuid(documentId)));
    }

    /** 특정 문서의 AI 설명 섹션 목록을 반환한다. */
    @GetMapping("/api/ai-documents/{documentId}/sections")
    public AiDocumentSectionsResponseDto getSections(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable String documentId,
            @RequestParam(required = false) String sectionType
    ) {
        User actor = queries.requireUser(userId(principal));
        return responseMapper.toSectionsResponse(
                queries.getSections(actor, parseUuid(documentId), sectionType)
        );
    }

    /** 특정 문서의 AI 설명 생성 상태를 반환한다. */
    @GetMapping("/api/ai-documents/{documentId}/explanation-status")
    public AiDocumentExplanationStatusResponseDto getExplanationStatus(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable String documentId
    ) {
        User actor = queries.requireUser(userId(principal));
        return responseMapper.toStatusResponse(
                queries.getExplanationStatus(actor, parseUuid(documentId))
        );
    }

    /** 경로 변수의 UUID 형식을 검증하고 UUID 객체로 변환한다. */
    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new com.gyote.silvercare.global.exception.BusinessException(
                    com.gyote.silvercare.medical_document.error.AiDocumentErrorCode.INVALID_FILTER
            );
        }
    }

    /** OAuth 인증 객체에서 내부 사용자 UUID를 추출한다. */
    private static UUID userId(OAuth2User user) {
        Object rawUserId = user.getAttributes().get("userId");
        if (rawUserId == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        try {
            return UUID.fromString(String.valueOf(rawUserId));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
    }
}
