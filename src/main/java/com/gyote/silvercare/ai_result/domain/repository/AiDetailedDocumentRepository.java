package com.gyote.silvercare.ai_result.domain.repository;
import com.gyote.silvercare.ai_result.domain.entity.AiDetailedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AiDetailedDocumentRepository extends JpaRepository<AiDetailedDocument, UUID> { Optional<AiDetailedDocument> findByResultDocumentId(UUID resultDocumentId); List<AiDetailedDocument> findAllByRequestedByUserIdOrderByCreatedAtDesc(UUID userId); }
