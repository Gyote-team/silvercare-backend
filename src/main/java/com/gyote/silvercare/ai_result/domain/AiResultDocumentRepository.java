package com.gyote.silvercare.ai_result.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface AiResultDocumentRepository extends JpaRepository<AiResultDocument, UUID> {

    // 요청 식별자로 AI 답변 문서를 조회한다.
    Optional<AiResultDocument> findByRequestId(UUID requestId);
}
