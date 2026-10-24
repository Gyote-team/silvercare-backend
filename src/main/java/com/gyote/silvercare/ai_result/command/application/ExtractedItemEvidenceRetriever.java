package com.gyote.silvercare.ai_result.command.application;

import com.gyote.silvercare.ai_result.domain.repository.ResultDocumentEvidenceRepository;
import com.gyote.silvercare.ai_result.infrastructure.AiResultGenerationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** 현재 분석 결과의 추출 항목을 근거로 사용하는 기본 검색 구현이다. */
@Component
@RequiredArgsConstructor
public class ExtractedItemEvidenceRetriever implements AiEvidenceRetriever {

    private final ResultDocumentEvidenceRepository evidenceRepository;

    /** 분석 완료된 추출 항목을 AI Backend 근거 모델로 변환한다. */
    @Override
    public List<AiResultGenerationClient.Evidence> retrieve(UUID patientId, List<UUID> documentIds) {
        return evidenceRepository.findEvidence(patientId, documentIds).stream()
                .map(this::toEvidence)
                .toList();
    }

    /** 저장소 조회 행을 citation 가능한 근거로 변환한다. */
    private AiResultGenerationClient.Evidence toEvidence(ResultDocumentEvidenceRepository.EvidenceRow row) {
        return new AiResultGenerationClient.Evidence(
                UUID.randomUUID(), "EXTRACTED_ITEM", UUID.fromString(row.getItemId()),
                UUID.fromString(row.getDocumentId()), row.getPageNo(), row.getLabel(), row.getSourceText(),
                row.getRawValue(), row.getUnit(), row.getReferenceRange()
        );
    }
}
