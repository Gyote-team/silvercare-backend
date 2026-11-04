package com.gyote.silvercare.ai_result.domain.repository;
import com.gyote.silvercare.ai_result.domain.entity.AiDetailedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface AiDetailedDocumentRepository extends JpaRepository<AiDetailedDocument, UUID> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 Optional<AiDetailedDocument> findByResultDocumentId(UUID resultDocumentId);
 List<AiDetailedDocument> findAllByRequestedByUserIdOrderByCreatedAtDesc(UUID userId);
}
