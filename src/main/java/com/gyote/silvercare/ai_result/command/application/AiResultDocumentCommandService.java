package com.gyote.silvercare.ai_result.command.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyote.silvercare.ai_result.domain.QuestionType;
import com.gyote.silvercare.ai_result.domain.entity.AiResultDocument;
import com.gyote.silvercare.ai_result.domain.repository.AiResultDocumentRepository;
import com.gyote.silvercare.ai_result.domain.repository.AiLlmUsageEventRepository;
import com.gyote.silvercare.ai_result.domain.entity.AiLlmUsageEvent;
import com.gyote.silvercare.ai_result.infrastructure.AiResultGenerationClient;
import com.gyote.silvercare.ai_result.query.model.AiResultDocumentView;
import com.gyote.silvercare.global.status.ResultStatus;
import com.gyote.silvercare.medical_document.query.application.CareRelationPermissionService;
import com.gyote.silvercare.user.domain.User;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 문서 근거를 수집해 AI Backend에 답변 생성을 요청하고 결과를 저장한다. */
@Service
public class AiResultDocumentCommandService {

    private final AiResultDocumentRepository resultDocuments;
    private final AiEvidenceRetriever evidenceRetriever;
    private final CareRelationPermissionService permissions;
    private final ObjectProvider<AiResultGenerationClient> generationClients;
    private final ObjectMapper objectMapper;
    private final AiLlmUsageEventRepository usageEvents;
    private final TransactionTemplate transactions;

    public AiResultDocumentCommandService(
            AiResultDocumentRepository resultDocuments,
            AiEvidenceRetriever evidenceRetriever,
            CareRelationPermissionService permissions,
            ObjectProvider<AiResultGenerationClient> generationClients,
            ObjectMapper objectMapper,
            AiLlmUsageEventRepository usageEvents,
            PlatformTransactionManager transactionManager
    ) {
        this.resultDocuments = resultDocuments;
        this.evidenceRetriever = evidenceRetriever;
        this.permissions = permissions;
        this.generationClients = generationClients;
        this.objectMapper = objectMapper;
        this.usageEvents = usageEvents;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** 접근 가능한 문서 근거를 모아 AI 답변을 생성하고 결과를 저장한다. */
    @Transactional(propagation = Propagation.NEVER)
    public AiResultDocumentView create(
            User actor,
            UUID requestedPatientId,
            String question,
            QuestionType questionType,
            List<UUID> documentIds
    ) {
        UUID patientId = permissions.resolveTargetPatient(actor, requestedPatientId);
        AiResultDocument result = transactions.execute(status -> resultDocuments.save(
                AiResultDocument.queued(patientId, actor.getId(), question, questionType)));
        UUID resultId = result.getId();
        List<AiResultGenerationClient.Evidence> evidence;
        try {
            evidence = evidenceRetriever.retrieve(patientId, documentIds);
        } catch (Exception exception) {
            return fail(resultId, "문서 근거를 조회하지 못했어요.");
        }
        if (evidence.isEmpty()) {
            return complete(resultId, ResultStatus.INSUFFICIENT, "근거 부족",
                    "선택한 문서에서 분석 완료된 근거를 찾지 못했어요.", "[]");
        }
        AiResultGenerationClient client = generationClients.getIfAvailable();
        if (client == null) {
            return fail(resultId, "AI 서버 설정이 없습니다.");
        }
        transactions.executeWithoutResult(status -> resultDocuments.findById(resultId)
                .orElseThrow().markRunning());
        AiResultGenerationClient.GenerationResponse generated;
        try {
            generated = client.generate(
                    new AiResultGenerationClient.GenerationRequest(
                            result.getRequestId(), patientId, actor.getId(), question, questionType, evidence
                    )
            );
        } catch (ResourceAccessException exception) {
            return fail(resultId, "AI 서버에 연결하지 못했어요. 서버 상태를 확인해 주세요.");
        } catch (RestClientResponseException exception) {
            return fail(resultId, "AI 서버가 요청을 처리하지 못했어요 (HTTP " + exception.getStatusCode().value() + ").");
        } catch (Exception exception) {
            return fail(resultId, "AI 응답 처리 중 오류가 발생했어요 (" + exception.getClass().getSimpleName() + ").");
        }
        try {
            recordUsage(resultId, generated);
            return applyGenerationResult(resultId, generated, evidence);
        } catch (Exception exception) {
            return fail(resultId, "AI 응답 처리 중 오류가 발생했어요 (" + exception.getClass().getSimpleName() + ").");
        }
    }

    private void recordUsage(UUID resultId, AiResultGenerationClient.GenerationResponse response) {
        AiResultGenerationClient.Usage usage = response.usage();
        transactions.executeWithoutResult(status -> {
            AiResultDocument result = resultDocuments.findById(resultId).orElseThrow();
            result.recordGenerationMetadata(response.promptVersion());
            result.recordClassification(response.questionType(), "RULE", null);
            usageEvents.save(AiLlmUsageEvent.answer(resultId, usage == null ? null : usage.model(),
                usage == null ? null : usage.inputTokens(), usage == null ? null : usage.outputTokens(),
                usage == null ? null : usage.totalTokens(), !response.failed(), response.errorCode()));
        });
    }

    /** AI Backend 응답 상태에 따라 결과 문서를 성공, 제한, 실패 상태로 갱신한다. */
    private AiResultDocumentView applyGenerationResult(
            UUID resultId,
            AiResultGenerationClient.GenerationResponse generated,
            List<AiResultGenerationClient.Evidence> evidence
    ) {
        if (generated.failed()) {
            String reason = generated.userMessage() == null || generated.userMessage().isBlank()
                    ? "AI 모델이 답변 생성에 실패했어요."
                    : generated.userMessage();
            return fail(resultId, reason + (generated.errorCode() == null ? "" : " (" + generated.errorCode() + ")"));
        }
        if (generated.blocked()) {
            return complete(resultId, ResultStatus.INSUFFICIENT, "답변 제한", generated.userMessage(), "[]");
        }
        return complete(resultId,
                ResultStatus.valueOf(generated.resultStatus()),
                generated.title(),
                generated.answer(),
                citations(generated, evidence)
        );
    }

    private AiResultDocumentView complete(
            UUID resultId, ResultStatus status, String title, String content, String citationJson
    ) {
        return transactions.execute(transaction -> {
            AiResultDocument result = resultDocuments.findById(resultId).orElseThrow();
            result.complete(status, title, content, citationJson);
            return AiResultDocumentView.from(result);
        });
    }

    private AiResultDocumentView fail(UUID resultId, String message) {
        return transactions.execute(transaction -> {
            AiResultDocument result = resultDocuments.findById(resultId).orElseThrow();
            result.fail(message);
            return AiResultDocumentView.from(result);
        });
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
