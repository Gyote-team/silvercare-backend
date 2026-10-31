package com.gyote.silvercare.ai_result.domain.repository;

import com.gyote.silvercare.ai_result.domain.entity.AiResultDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import com.gyote.silvercare.ai_result.domain.DocumentSaveStatus;

public interface AiResultDocumentRepository extends JpaRepository<AiResultDocument, UUID> {
    List<AiResultDocument> findAllByRequestedByUserIdAndSaveStatusOrderBySavedAtDesc(
            UUID userId, DocumentSaveStatus saveStatus);
}
