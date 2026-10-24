package com.gyote.silvercare.ai_result.query.model;

import com.gyote.silvercare.ai_result.domain.entity.AiResultDocument;
import com.gyote.silvercare.global.status.AiJobStatus;

import java.util.UUID;

/** API 계층에 의존하지 않는 AI 답변 문서 조회 결과다. */
public record AiResultDocumentView(
        UUID resultDocumentId,
        AiJobStatus jobStatus,
        String content,
        String citationJson,
        String errorMessage
) {

    /** AI 답변 문서 엔티티를 조회 결과로 변환한다. */
    public static AiResultDocumentView from(AiResultDocument result) {
        return new AiResultDocumentView(
                result.getId(), result.getJobStatus(), result.getContent(),
                result.getCitationJson(), result.getErrorMessage()
        );
    }
}
