package com.gyote.silvercare.medical_document.api.controller;

import com.gyote.silvercare.medical_document.api.dto.request.MedicalDocumentListRequestDto;
import com.gyote.silvercare.medical_document.api.dto.response.MedicalDocumentDetailResponseDto;
import com.gyote.silvercare.medical_document.api.dto.response.MedicalDocumentListResponseDto;
import com.gyote.silvercare.medical_document.api.mapper.MedicalDocumentResponseMapper;
import com.gyote.silvercare.medical_document.query.application.MedicalDocumentQueryService;
import com.gyote.silvercare.user.domain.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class MedicalDocumentQueryController {

    private final MedicalDocumentQueryService queries;
    private final MedicalDocumentResponseMapper responseMapper;

    public MedicalDocumentQueryController(
            MedicalDocumentQueryService queries,
            MedicalDocumentResponseMapper responseMapper
    ) {
        this.queries = queries;
        this.responseMapper = responseMapper;
    }

    @GetMapping("/api/documents")
    public MedicalDocumentListResponseDto list(
            @AuthenticationPrincipal OAuth2User user,
            MedicalDocumentListRequestDto request
    ) {
        User me = queries.requireUser(kakaoId(user));
        return responseMapper.toPageResponse(queries.list(
                me, request.patientId(), request.visitId(), request.cursor(), request.size()));
    }

    @GetMapping("/api/documents/{documentId}")
    public MedicalDocumentDetailResponseDto get(
            @AuthenticationPrincipal OAuth2User user,
            @PathVariable UUID documentId
    ) {
        User me = queries.requireUser(kakaoId(user));
        return responseMapper.toResponse(queries.get(me, documentId));
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }
}
