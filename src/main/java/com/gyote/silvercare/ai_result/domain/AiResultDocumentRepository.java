package com.gyote.silvercare.ai_result.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface AiResultDocumentRepository extends JpaRepository<AiResultDocument, UUID> {

    Optional<AiResultDocument> findByRequestId(UUID requestId);
}
