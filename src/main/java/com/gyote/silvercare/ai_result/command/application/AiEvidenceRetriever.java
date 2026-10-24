package com.gyote.silvercare.ai_result.command.application;

import com.gyote.silvercare.ai_result.infrastructure.AiResultGenerationClient;

import java.util.List;
import java.util.UUID;

/** 질문에 사용할 권한 검증 근거를 조회해 AI Backend 요청 모델로 변환한다. */
public interface AiEvidenceRetriever {

    /** 대상 환자와 선택 문서 범위에서 답변 근거를 조회한다. */
    List<AiResultGenerationClient.Evidence> retrieve(UUID patientId, List<UUID> documentIds);
}
