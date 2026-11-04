package com.gyote.silvercare.ai_result.domain.repository;

import com.gyote.silvercare.ai_result.domain.entity.AiResultDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.UUID;
import java.util.List;
import com.gyote.silvercare.ai_result.domain.DocumentSaveStatus;

public interface AiResultDocumentRepository extends JpaRepository<AiResultDocument, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AiResultDocument r where r.id = :id")
    java.util.Optional<AiResultDocument> findByIdForUpdate(@Param("id") UUID id);

    List<AiResultDocument> findAllByRequestedByUserIdAndSaveStatusOrderBySavedAtDesc(
            UUID userId, DocumentSaveStatus saveStatus);
}
