package com.gyote.silvercare.medical_document.domain.entity;

import com.gyote.silvercare.medical_document.domain.DocumentAnalysisStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentAnalysisTest {

    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");

    @Test
    void queuedStartsPendingWithoutRetry() {
        DocumentAnalysis analysis = queuedAnalysis();

        assertThat(analysis.getId()).isNotNull();
        assertThat(analysis.getDocumentId()).isEqualTo(DOCUMENT_ID);
        assertThat(analysis.getParserVersion()).isEqualTo("parser-v1");
        assertThat(analysis.getStatus()).isEqualTo(DocumentAnalysisStatus.PENDING);
        assertThat(analysis.getRetryCount()).isZero();
        assertThat(analysis.isRetryable()).isFalse();
    }

    @Test
    void recordRetryIncreasesRetryCount() {
        DocumentAnalysis analysis = queuedAnalysis();

        analysis.recordRetry();

        assertThat(analysis.getRetryCount()).isEqualTo(1);
    }

    @Test
    void failRecordsErrorFields() {
        DocumentAnalysis analysis = queuedAnalysis();

        analysis.fail("AI_TIMEOUT", "ANALYSIS_REQUEST", "AI 서버 응답 시간 초과", true, NOW);

        assertThat(analysis.getStatus()).isEqualTo(DocumentAnalysisStatus.FAILED);
        assertThat(analysis.isRetryable()).isTrue();
        assertThat(analysis.getErrorCode()).isEqualTo("AI_TIMEOUT");
        assertThat(analysis.getFailedStep()).isEqualTo("ANALYSIS_REQUEST");
        assertThat(analysis.getErrorMessage()).isEqualTo("AI 서버 응답 시간 초과");
        assertThat(analysis.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void failWithNonRetryableErrorIsNotRetryable() {
        DocumentAnalysis analysis = queuedAnalysis();

        analysis.fail("INTERNAL_AUTH_FAILED", "ANALYSIS_REQUEST", "내부 인증 실패", false, NOW);

        assertThat(analysis.isRetryable()).isFalse();
    }

    private DocumentAnalysis queuedAnalysis() {
        return DocumentAnalysis.queued(DOCUMENT_ID, "parser-v1");
    }
}
