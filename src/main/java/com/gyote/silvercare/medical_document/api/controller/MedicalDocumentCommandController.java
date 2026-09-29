package com.gyote.silvercare.medical_document.api.controller;

import com.gyote.silvercare.medical_document.command.application.MedicalDocumentCommandService;
import com.gyote.silvercare.medical_document.query.application.MedicalDocumentQueryService;
import com.gyote.silvercare.user.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class MedicalDocumentCommandController {

    private final MedicalDocumentQueryService queries;
    private final MedicalDocumentCommandService commands;

    public MedicalDocumentCommandController(
            MedicalDocumentQueryService queries,
            MedicalDocumentCommandService commands
    ) {
        this.queries = queries;
        this.commands = commands;
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
