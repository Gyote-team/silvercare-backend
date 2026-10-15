package com.gyote.silvercare.ai_result.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gyote.silvercare.ai_result.domain.QuestionType;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

public class AiResultWorkerClient {

    private static final String TOKEN_HEADER = "X-Internal-Token";
    private final RestClient restClient;

    // 내부 인증 토큰을 포함한 AI Backend 전용 HTTP 클라이언트를 구성한다.
    public AiResultWorkerClient(RestClient.Builder builder, String baseUrl, String token) {
        this.restClient = builder.baseUrl(baseUrl.replaceAll("/+$", ""))
                .defaultHeader(TOKEN_HEADER, token)
                .build();
    }

    // AI Backend에 문서 근거와 질문을 전달해 답변 생성을 요청한다.
    public WorkerResponse generate(WorkerRequest request) {
        return restClient.post()
                .uri("/internal/ai-result-documents/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(WorkerResponse.class);
    }

    public record WorkerRequest(
            @JsonProperty("request_id") UUID requestId,
            @JsonProperty("patient_id") UUID patientId,
            @JsonProperty("actor_user_id") UUID actorUserId,
            String question,
            @JsonProperty("question_type_hint") QuestionType questionTypeHint,
            List<Evidence> evidence
    ) {
    }

    public record Evidence(
            @JsonProperty("citation_id") UUID citationId,
            @JsonProperty("source_type") String sourceType,
            @JsonProperty("source_id") UUID sourceId,
            @JsonProperty("document_id") UUID documentId,
            @JsonProperty("page_no") Integer pageNo,
            String label,
            @JsonProperty("source_text") String sourceText,
            String value,
            String unit,
            @JsonProperty("reference_range") String referenceRange
    ) {
    }

    public record WorkerResponse(
            @JsonProperty("request_id") UUID requestId,
            @JsonProperty("result_status") String resultStatus,
            String title,
            String answer,
            List<Sentence> sentences,
            boolean blocked,
            boolean failed,
            @JsonProperty("error_code") String errorCode,
            boolean retryable,
            @JsonProperty("user_message") String userMessage
    ) {
    }

    public record Sentence(String text, @JsonProperty("citation_ids") List<UUID> citationIds) {
    }
}
