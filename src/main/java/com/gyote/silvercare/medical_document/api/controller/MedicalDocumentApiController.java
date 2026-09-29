package com.gyote.silvercare.medical_document.api.controller;

import com.gyote.silvercare.medical_document.api.dto.response.MedicalDocumentPageResponse;
import com.gyote.silvercare.medical_document.api.dto.response.MedicalDocumentResponse;
import com.gyote.silvercare.medical_document.api.mapper.MedicalDocumentResponseMapper;
import com.gyote.silvercare.medical_document.command.application.MedicalDocumentCommandService;
import com.gyote.silvercare.medical_document.query.application.MedicalDocumentQueryService;
import com.gyote.silvercare.user.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class MedicalDocumentApiController {

    private final MedicalDocumentQueryService queries;
    private final MedicalDocumentCommandService commands;
    private final MedicalDocumentResponseMapper responseMapper;

    public MedicalDocumentApiController(
            MedicalDocumentQueryService queries,
            MedicalDocumentCommandService commands,
            MedicalDocumentResponseMapper responseMapper
    ) {
        this.queries = queries;
        this.commands = commands;
        this.responseMapper = responseMapper;
    }

    @GetMapping("/api/documents")
    public MedicalDocumentPageResponse list(
            @AuthenticationPrincipal OAuth2User user,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        User me = queries.requireUser(kakaoId(user));
        return responseMapper.toPageResponse(queries.list(me, patientId, visitId, cursor, size));
    }

    @GetMapping("/api/documents/{documentId}")
    public MedicalDocumentResponse get(
            @AuthenticationPrincipal OAuth2User user,
            @PathVariable UUID documentId
    ) {
        User me = queries.requireUser(kakaoId(user));
        return responseMapper.toResponse(queries.get(me, documentId));
    }

    @DeleteMapping("/api/documents/{documentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal OAuth2User user,
            @PathVariable UUID documentId
    ) {
        User me = queries.requireUser(kakaoId(user));
        commands.delete(me, documentId);
        return ResponseEntity.noContent().build();
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }
}
