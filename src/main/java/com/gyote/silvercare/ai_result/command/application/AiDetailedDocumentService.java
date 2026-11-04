package com.gyote.silvercare.ai_result.command.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyote.silvercare.ai_result.domain.entity.*;
import com.gyote.silvercare.ai_result.domain.repository.*;
import com.gyote.silvercare.ai_result.infrastructure.AiResultGenerationClient;
import com.gyote.silvercare.medical_document.domain.DocumentStoragePort;
import com.gyote.silvercare.medical_document.query.application.CareRelationPermissionService;
import com.gyote.silvercare.ai_result.domain.DocumentSaveStatus;
import com.gyote.silvercare.ai_result.error.AiResultErrorCode;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.global.status.ResultStatus;
import com.gyote.silvercare.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 저장을 확정한 답변만 별도 GPT 호출로 상세 문서와 PDF로 만든다. */
@Service @RequiredArgsConstructor
public class AiDetailedDocumentService {
 private final AiDetailedDocumentRepository detailed; private final AiResultDocumentRepository results;
 private final AiEvidenceRetriever evidenceRetriever; private final ObjectProvider<AiResultGenerationClient> clients; private final DocumentStoragePort storage;
 private final CareRelationPermissionService permissions;
 private final ObjectMapper objectMapper;
 @Value("${silvercare.pdf.font-path:}") private String pdfFontPath;
 @Transactional
 public AiDetailedDocument create(User actor, UUID resultId) {
   var source=results.findById(resultId).orElseThrow();
   if(!source.getRequestedByUserId().equals(actor.getId())) throw new BusinessException(AiResultErrorCode.RESULT_FORBIDDEN);
   permissions.requireDocumentAccess(actor, source.getPatientId());
   if(source.getSaveStatus()!=DocumentSaveStatus.SAVED || source.getJobStatus()!=AiJobStatus.SUCCEEDED ||
           (source.getResultStatus()!=ResultStatus.COMPLETE && source.getResultStatus()!=ResultStatus.PARTIAL))
     throw new BusinessException(AiResultErrorCode.RESULT_NOT_SAVABLE);
   var existing=detailed.findByResultDocumentId(resultId); if(existing.isPresent() && existing.get().getStatus().name().equals("READY")) return existing.get();
   var doc=existing.orElseGet(() -> detailed.save(AiDetailedDocument.generating(resultId, actor.getId())));
   try {
     var client=clients.getIfAvailable(); if(client==null) throw new IllegalStateException("AI 서버 설정이 없습니다.");
     var evidence=evidenceRetriever.retrieve(source.getPatientId(), sourceDocumentIds(source.getCitationJson()));
     if(evidence.isEmpty()) throw new IllegalStateException("상세 문서 생성에 사용할 근거가 없습니다.");
     var response=client.generateDetail(new AiResultGenerationClient.GenerationRequest(source.getRequestId(), source.getPatientId(), actor.getId(), source.getQuestion(), source.getQuestionType(), evidence));
     if(response.failed() || response.blocked() || response.answer() == null || response.answer().isBlank()) throw new IllegalStateException(response.userMessage()==null?"상세 문서 답변이 비어 있습니다.":response.userMessage());
     String content=response.answer(); String title=response.title(); String key="ai-detailed/"+doc.getId()+"/report.pdf";
     storage.storeOriginal(key, SimplePdfRenderer.render(title, content, pdfFontPath), "application/pdf");
     String citationJson = citations(response, evidence);
     if (citationJson.equals("[]")) throw new IllegalStateException("상세 문서 답변의 근거를 확인하지 못했습니다.");
     doc.complete(title, content, citationJson, key, safeFileName(title)+".pdf"); return doc;
   } catch(Exception e) { doc.fail(e.getMessage()==null?"상세 문서 생성에 실패했습니다.":e.getMessage()); return doc; }
 }
 @Transactional(readOnly=true) public AiDetailedDocument get(User actor, UUID id) { var d=detailed.findById(id).orElseThrow(); if(!d.getRequestedByUserId().equals(actor.getId())) throw new BusinessException(AiResultErrorCode.RESULT_FORBIDDEN); var source=results.findById(d.getResultDocumentId()).orElseThrow(); permissions.requireDocumentAccess(actor, source.getPatientId()); return d; }
 private static String safeFileName(String title){ return (title==null||title.isBlank()?"AI_상세설명":title).replaceAll("[\\\\/:*?\"<>|]", "_"); }
 private String citations(AiResultGenerationClient.GenerationResponse response, List<AiResultGenerationClient.Evidence> evidence) {
   try {
     Set<UUID> allowed = evidence.stream().map(AiResultGenerationClient.Evidence::citationId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
     List<AiResultGenerationClient.Sentence> validSentences = response.sentences()==null ? List.of() : response.sentences().stream()
             .filter(Objects::nonNull)
             .filter(sentence -> sentence.citationIds()!=null && !sentence.citationIds().isEmpty())
             .filter(sentence -> allowed.containsAll(sentence.citationIds()))
             .toList();
     return validSentences.isEmpty() ? "[]" : objectMapper.writeValueAsString(Map.of("sentences", validSentences, "sources", evidence));
   }
   catch (Exception ignored) { return "[]"; }
 }
 private List<UUID> sourceDocumentIds(String citationJson) {
   if (citationJson == null || citationJson.isBlank()) return List.of();
   try {
     JsonNode sources = objectMapper.readTree(citationJson).path("sources");
     if (!sources.isArray()) return List.of();
     List<UUID> ids = new ArrayList<>();
     for (JsonNode source : sources) {
       String value = source.path("document_id").asText(null);
       if (value != null) try { ids.add(UUID.fromString(value)); } catch (IllegalArgumentException ignored) { }
     }
     return ids.stream().distinct().toList();
   } catch (Exception ignored) { return List.of(); }
 }
}
