package com.gyote.silvercare.medical_document.domain.entity;

import com.gyote.silvercare.medical_document.domain.DocumentAnalysisStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 의료문서 원본을 파싱·분석한 결과와 분석 처리 상태를 관리하는 엔티티입니다. */
@Entity
@Table(name = "document_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentAnalysis {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "parser_version", nullable = false, length = 50)
    private String parserVersion;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentAnalysisStatus status;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    // H2 테스트 스키마에도 DEFAULT가 생기도록 선언합니다. 이 컬럼을 빼고 INSERT하는 기존 테스트가 있습니다.
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean retryable;

    @ColumnDefault("0")
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "failed_step", length = 100)
    private String failedStep;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** 업로드 직후 대기 중인 분석 작업을 만듭니다. 상태는 PENDING이고 재시도 횟수는 0입니다. */
    public static DocumentAnalysis queued(UUID documentId, String parserVersion) {
        DocumentAnalysis analysis = new DocumentAnalysis();
        analysis.id = UUID.randomUUID();
        analysis.documentId = documentId;
        analysis.parserVersion = parserVersion;
        analysis.status = DocumentAnalysisStatus.PENDING;
        analysis.retryable = false;
        analysis.retryCount = 0;
        return analysis;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** 분석 요청을 다시 보낼 때 재시도 횟수를 1 늘립니다. */
    public void recordRetry() {
        retryCount++;
    }

    /** 분석을 최종 실패로 기록합니다. 오류 코드·실패 단계·메시지·완료 시각을 남기고 더 이상 재시도하지 않습니다. */
    public void fail(String errorCode, String failedStep, String message, Instant now) {
        status = DocumentAnalysisStatus.FAILED;
        retryable = false;
        this.errorCode = errorCode;
        this.failedStep = failedStep;
        errorMessage = message;
        completedAt = now;
    }
}
