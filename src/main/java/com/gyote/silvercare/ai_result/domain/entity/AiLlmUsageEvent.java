package com.gyote.silvercare.ai_result.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_llm_usage_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiLlmUsageEvent {
    @Id private UUID id;
    @Column(name = "result_document_id") private UUID resultDocumentId;
    @Column(nullable = false) private String purpose;
    @Column(nullable = false) private String provider;
    private String model;
    @Column(name = "input_tokens") private Integer inputTokens;
    @Column(name = "output_tokens") private Integer outputTokens;
    @Column(name = "total_tokens") private Integer totalTokens;
    @Column(nullable = false) private boolean success;
    @Column(name = "error_code") private String errorCode;
    @Column(name = "called_at", nullable = false) private Instant calledAt;

    public static AiLlmUsageEvent answer(UUID resultId, String model, Integer input, Integer output,
                                         Integer total, boolean success, String errorCode) {
        AiLlmUsageEvent event = new AiLlmUsageEvent();
        event.id = UUID.randomUUID(); event.resultDocumentId = resultId; event.purpose = "ANSWER";
        event.provider = "OPENAI"; event.model = model; event.inputTokens = input;
        event.outputTokens = output; event.totalTokens = total; event.success = success;
        event.errorCode = errorCode; event.calledAt = Instant.now();
        return event;
    }
}
