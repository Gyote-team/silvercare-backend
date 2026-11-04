package com.gyote.silvercare.ai_result.domain.repository;
import com.gyote.silvercare.ai_result.domain.entity.AiDetailedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface AiDetailedDocumentRepository extends JpaRepository<AiDetailedDocument, UUID> {
 Optional<AiDetailedDocument> findByResultDocumentId(UUID resultDocumentId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select d from AiDetailedDocument d where d.resultDocumentId = :resultDocumentId")
 Optional<AiDetailedDocument> findByResultDocumentIdForUpdate(@Param("resultDocumentId") UUID resultDocumentId);
 List<AiDetailedDocument> findAllByRequestedByUserIdOrderByCreatedAtDesc(UUID userId);
}
