package com.gyote.silvercare.ai_result.command.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyote.silvercare.ai_result.domain.QuestionType;
import com.gyote.silvercare.ai_result.domain.entity.AiResultDocument;
import com.gyote.silvercare.ai_result.domain.repository.AiResultDocumentRepository;
import com.gyote.silvercare.ai_result.domain.repository.ResultDocumentEvidenceRepository;
import com.gyote.silvercare.ai_result.infrastructure.AiResultGenerationClient;
import com.gyote.silvercare.ai_result.query.model.AiResultDocumentView;
import com.gyote.silvercare.global.status.ResultStatus;
import com.gyote.silvercare.medical_document.query.application.CareRelationPermissionService;
import com.gyote.silvercare.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 문서 근거를 수집해 AI Backend에 답변 생성을 요청하고 결과를 저장한다. */
@Service
@RequiredArgsConstructor
public class AiResultDocumentCommandService {

    private final AiResultDocumentRepository resultDocuments;
    private final ResultDocumentEvidenceRepository evidenceRepository;
    private final CareRelationPermissionService permissions;
    private final ObjectProvider<AiResultGenerationClient> generationClients;
    private final ObjectMapper objectMapper;

    /** 접근 가능한 문서 근거를 모아 AI 답변을 생성하고 결과를 저장한다. */
    @Transactional
    public AiResultDocumentView create(
            User actor,
            UUID requestedPatientId,
            String question,
            QuestionType questionType,
            List<UUID> documentIds
    ) {
        UUID patientId = permissions.resolveTargetPatient(actor, requestedPatientId);
        AiResultDocument result = resultDocuments.save(
                AiResultDocument.queued(patientId, actor.getId(), question, questionType)
        );
        List<ResultDocumentEvidenceRepository.EvidenceRow> rows =
                evidenceRepository.findEvidence(patientId, documentIds);
        if (rows.isEmpty()) {
            result.complete(ResultStatus.INSUFFICIENT, "근거 부족", "선택한 문서에서 분석 완료된 근거를 찾지 못했어요.", "[]");
            return AiResultDocumentView.from(result);
        }
        AiResultGenerationClient client = generationClients.getIfAvailable();
        if (client == null) {
            result.fail("AI 서버 설정이 없습니다.");
            return AiResultDocumentView.from(result);
        }
        result.markRunning();
        try {
            List<AiResultGenerationClient.Evidence> evidence = rows.stream().map(row ->
                    new AiResultGenerationClient.Evidence(
                            UUID.randomUUID(), "EXTRACTED_ITEM", UUID.fromString(row.getItemId()),
                            UUID.fromString(row.getDocumentId()), row.getPageNo(), row.getLabel(), row.getSourceText(),
                            row.getRawValue(), row.getUnit(), row.getReferenceRange()
                    )).toList();
            AiResultGenerationClient.GenerationResponse generated = client.generate(
                    new AiResultGenerationClient.GenerationRequest(
                            result.getRequestId(), patientId, actor.getId(), question, questionType, evidence
                    )
            );
            applyGenerationResult(result, generated, evidence);
        } catch (ResourceAccessException exception) {
            result.fail("AI 서버에 연결하지 못했어요. 서버 상태를 확인해 주세요.");
        } catch (RestClientResponseException exception) {
            result.fail("AI 서버가 요청을 처리하지 못했어요 (HTTP " + exception.getStatusCode().value() + ").");
        } catch (Exception exception) {
            result.fail("AI 응답 처리 중 오류가 발생했어요 (" + exception.getClass().getSimpleName() + ").");
        }
        return AiResultDocumentView.from(result);
    }

    /** AI Backend 응답 상태에 따라 결과 문서를 성공, 제한, 실패 상태로 갱신한다. */
    private void applyGenerationResult(
            AiResultDocument result,
            AiResultGenerationClient.GenerationResponse generated,
            List<AiResultGenerationClient.Evidence> evidence
    ) {
        if (generated.failed()) {
            String reason = generated.userMessage() == null || generated.userMessage().isBlank()
                    ? "AI 모델이 답변 생성에 실패했어요."
                    : generated.userMessage();
            result.fail(reason + (generated.errorCode() == null ? "" : " (" + generated.errorCode() + ")"));
            return;
        }
        if (generated.blocked()) {
            result.complete(ResultStatus.INSUFFICIENT, "답변 제한", generated.userMessage(), "[]");
            return;
        }
        result.complete(
                ResultStatus.valueOf(generated.resultStatus()),
                generated.title(),
                generated.answer(),
                citations(generated, evidence)
        );
    }

    /** AI 답변 문장과 원문 근거를 JSON 형태로 직렬화한다. */
    private String citations(
            AiResultGenerationClient.GenerationResponse response,
            List<AiResultGenerationClient.Evidence> evidence
    ) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "sentences", response.sentences(),
                    "sources", evidence
            ));
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }
}
