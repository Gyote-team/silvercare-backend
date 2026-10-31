package com.gyote.silvercare.ai_result.api.controller;
import com.gyote.silvercare.ai_result.command.application.AiDetailedDocumentService;
import com.gyote.silvercare.ai_result.domain.entity.AiDetailedDocument;
import com.gyote.silvercare.medical_document.domain.DocumentStoragePort;
import com.gyote.silvercare.user.domain.User; import com.gyote.silvercare.user.query.application.UserQueryService;
import lombok.RequiredArgsConstructor; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.core.user.OAuth2User; import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.view.RedirectView; import java.time.Duration; import java.util.*;
@RestController @RequestMapping("/api/ai-detailed-documents") @RequiredArgsConstructor
public class AiDetailedDocumentController {
 private final AiDetailedDocumentService service; private final UserQueryService users; private final DocumentStoragePort storage;
 @PostMapping("/from-result/{resultDocumentId}") public View create(@AuthenticationPrincipal OAuth2User p,@PathVariable UUID resultDocumentId){return View.of(service.create(actor(p),resultDocumentId));}
 @GetMapping("/{id}") public View get(@AuthenticationPrincipal OAuth2User p,@PathVariable UUID id){return View.of(service.get(actor(p),id));}
 @GetMapping("/{id}/download") public RedirectView download(@AuthenticationPrincipal OAuth2User p,@PathVariable UUID id){var d=service.get(actor(p),id); if(d.getPdfStorageKey()==null) throw new IllegalStateException("PDF가 준비되지 않았습니다."); return new RedirectView(storage.createSignedUrl(d.getPdfStorageKey(),Duration.ofMinutes(10)));}
 private User actor(OAuth2User p){return users.requireById(UUID.fromString(String.valueOf(p.getAttributes().get("userId"))));}
 public record View(UUID detailedDocumentId,String status,String title,String content,String citationJson,String pdfFileName,String errorMessage){static View of(AiDetailedDocument d){return new View(d.getId(),d.getStatus().name(),d.getTitle(),d.getContent(),d.getCitationJson(),d.getPdfFileName(),d.getErrorMessage());}}
}
