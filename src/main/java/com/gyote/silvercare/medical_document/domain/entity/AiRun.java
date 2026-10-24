package com.gyote.silvercare.medical_document.domain.entity;

import com.gyote.silvercare.global.status.AiJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** 문서 분석 또는 설명 생성을 수행하는 AI 작업의 진행 상태를 관리하는 엔티티입니다. */
@Entity
@Table(name = "ai_runs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRun {

    @Id
    private UUID id;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "analysis_id")
    private UUID analysisId;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "run_type", nullable = false, length = 30)
    private String runType;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiJobStatus status;

    @Column(name = "current_step", length = 50)
    private String currentStep;

    private Integer progress;

    @Column(nullable = false)
    private boolean retryable;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "failed_step", length = 100)
    private String failedStep;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
