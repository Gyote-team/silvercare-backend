package com.gyote.silvercare.ai_result.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyote.silvercare.ai_result.api.dto.AiResultDocumentResponse;
import com.gyote.silvercare.ai_result.api.dto.CreateAiResultDocumentRequest;
import com.gyote.silvercare.ai_result.domain.AiResultDocument;
import com.gyote.silvercare.ai_result.domain.AiResultDocumentRepository;
import com.gyote.silvercare.ai_result.domain.ResultDocumentEvidenceRepository;
import com.gyote.silvercare.ai_result.infrastructure.AiResultWorkerClient;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.ResultStatus;
import com.gyote.silvercare.medical_document.error.AiDocumentErrorCode;
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

@Service
@RequiredArgsConstructor
public class AiResultDocumentService {

    private final AiResultDocumentRepository resultDocuments;
    private final ResultDocumentEvidenceRepository evidenceRepository;
    private final CareRelationPermissionService permissions;
    private final ObjectProvider<AiResultWorkerClient> workers;
    private final ObjectMapper objectMapper;

    // 접근 가능한 문서 근거를 모아 AI 답변을 생성하고 결과를 저장한다.
    @Transactional
    public AiResultDocumentResponse create(User actor, CreateAiResultDocumentRequest request) {
        UUID patientId = permissions.resolveTargetPatient(actor, request.patientId());
        AiResultDocument result = resultDocuments.save(
                AiResultDocument.queued(patientId, actor.getId(), request.question(), request.questionType())
        );
        List<ResultDocumentEvidenceRepository.EvidenceRow> rows = evidenceRepository.findEvidence(
                patientId, request.documentIds()
        );
        if (rows.isEmpty()) {
            result.complete(ResultStatus.INSUFFICIENT, "근거 부족", "선택한 문서에서 분석 완료된 근거를 찾지 못했어요.", "[]");
            return response(result);
        }
        AiResultWorkerClient worker = workers.getIfAvailable();
        if (worker == null) {
            result.fail("AI 서버 설정이 없습니다.");
            return response(result);
        }
        result.markRunning();
        try {
            List<AiResultWorkerClient.Evidence> evidence = rows.stream().map(row -> new AiResultWorkerClient.Evidence(
                    UUID.randomUUID(), "EXTRACTED_ITEM", UUID.fromString(row.getItemId()),
                    UUID.fromString(row.getDocumentId()), row.getPageNo(), row.getLabel(), row.getSourceText(),
                    row.getRawValue(), row.getUnit(), row.getReferenceRange()
            )).toList();
            AiResultWorkerClient.WorkerResponse workerResponse = worker.generate(new AiResultWorkerClient.WorkerRequest(
                    result.getRequestId(), patientId, actor.getId(), request.question(), request.questionType(), evidence
            ));
            if (workerResponse.failed()) {
                String reason = workerResponse.userMessage() == null || workerResponse.userMessage().isBlank()
                        ? "AI 모델이 답변 생성에 실패했어요."
                        : workerResponse.userMessage();
                result.fail(reason + (workerResponse.errorCode() == null ? "" : " (" + workerResponse.errorCode() + ")"));
            } else if (workerResponse.blocked()) {
                result.complete(ResultStatus.INSUFFICIENT, "답변 제한", workerResponse.userMessage(), "[]");
            } else {
                result.complete(
                        ResultStatus.valueOf(workerResponse.resultStatus()),
                        workerResponse.title(),
                        workerResponse.answer(),
                        citations(workerResponse, evidence)
                );
            }
        } catch (ResourceAccessException exception) {
            result.fail("AI 서버에 연결하지 못했어요. 서버 상태를 확인해 주세요.");
        } catch (RestClientResponseException exception) {
            result.fail("AI 서버가 요청을 처리하지 못했어요 (HTTP " + exception.getStatusCode().value() + ").");
        } catch (Exception exception) {
            result.fail("AI 응답 처리 중 오류가 발생했어요 (" + exception.getClass().getSimpleName() + ").");
        }
        return response(result);
    }

    // 접근 권한을 확인한 뒤 AI 답변 문서를 조회한다.
    @Transactional(readOnly = true)
    public AiResultDocumentResponse get(User actor, UUID resultDocumentId) {
        AiResultDocument result = resultDocuments.findById(resultDocumentId)
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND));
        permissions.requireDocumentAccess(actor, result.getPatientId());
        return response(result);
    }

    // 요청 식별자로 AI 답변 생성 상태와 결과를 조회한다.
    @Transactional(readOnly = true)
    public AiResultDocumentResponse getByRequestId(User actor, UUID requestId) {
        AiResultDocument result = resultDocuments.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_NOT_FOUND));
        permissions.requireDocumentAccess(actor, result.getPatientId());
        return response(result);
    }

    // AI 답변 문장과 원문 근거를 JSON 형태로 직렬화한다.
    private String citations(
            AiResultWorkerClient.WorkerResponse response,
            List<AiResultWorkerClient.Evidence> evidence
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

    // AI 답변 문서 엔티티를 API 응답 형태로 변환한다.
    private static AiResultDocumentResponse response(AiResultDocument result) {
        return new AiResultDocumentResponse(
                result.getId(), result.getRequestId(), result.getPatientId(), result.getQuestionType(),
                result.getJobStatus(), result.getResultStatus(), result.getTitle(), result.getContent(),
                result.getCitationJson(), result.getErrorMessage(), result.getCreatedAt(), result.getCompletedAt(),
                "/api/ai-operations/" + result.getRequestId()
        );
    }
}
