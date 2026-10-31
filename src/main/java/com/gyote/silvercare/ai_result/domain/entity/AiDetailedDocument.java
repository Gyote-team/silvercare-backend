package com.gyote.silvercare.ai_result.domain.entity;

import com.gyote.silvercare.ai_result.domain.DetailedDocumentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ai_detailed_documents") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiDetailedDocument {
 @Id private UUID id; @Column(name="result_document_id", nullable=false, unique=true) private UUID resultDocumentId;
 @Column(name="requested_by_user_id", nullable=false) private UUID requestedByUserId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private DetailedDocumentStatus status;
 private String title; @Column(columnDefinition="TEXT") private String content; @Column(name="citation_json", columnDefinition="TEXT") private String citationJson;
 @Column(name="pdf_storage_key") private String pdfStorageKey; @Column(name="pdf_file_name") private String pdfFileName; @Column(name="error_message") private String errorMessage;
 @Column(name="created_at", nullable=false) private Instant createdAt; @Column(name="completed_at") private Instant completedAt;
 public static AiDetailedDocument generating(UUID resultId, UUID owner) { var d=new AiDetailedDocument(); d.id=UUID.randomUUID(); d.resultDocumentId=resultId; d.requestedByUserId=owner; d.status=DetailedDocumentStatus.GENERATING; d.createdAt=Instant.now(); return d; }
 public void complete(String title,String content,String citations,String key,String file) { status=DetailedDocumentStatus.READY; this.title=title; this.content=content; citationJson=citations; pdfStorageKey=key; pdfFileName=file; completedAt=Instant.now(); }
 public void fail(String message) { status=DetailedDocumentStatus.FAILED; errorMessage=message; completedAt=Instant.now(); }
}
