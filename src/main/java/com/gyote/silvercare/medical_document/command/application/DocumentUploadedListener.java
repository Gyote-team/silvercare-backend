package com.gyote.silvercare.medical_document.command.application;

import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.medical_document.domain.AnalysisFailureType;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisPort;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisRequest;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisResult;
import com.gyote.silvercare.medical_document.domain.DocumentUploadedEvent;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 문서 업로드가 커밋된 뒤 AI 서버에 분석 시작을 요청하는 리스너입니다.
 * 업로드 응답이 AI 서버를 기다리지 않도록 별도 스레드에서 돌고, 실패하면 재시도한 뒤 최종 실패를 기록합니다.
 */
@Component
public class DocumentUploadedListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadedListener.class);

    private final MedicalDocumentRepository documents;
    private final DocumentAnalysisPort analysisPort;
    private final DocumentAnalysisStateCommandService states;
    private final int maxAttempts;
    private final Duration retryBackoff;

    public DocumentUploadedListener(
            MedicalDocumentRepository documents,
            DocumentAnalysisPort analysisPort,
            DocumentAnalysisStateCommandService states,
            @Value("${silvercare.ai.max-attempts:3}") int maxAttempts,
            @Value("${silvercare.ai.retry-backoff:500ms}") Duration retryBackoff
    ) {
        this.documents = documents;
        this.analysisPort = analysisPort;
        this.states = states;
        this.maxAttempts = maxAttempts;
        this.retryBackoff = retryBackoff;
    }

    /** UPLOADED 상태인 문서의 분석 시작을 요청합니다. 예상 밖 예외는 밖으로 던지지 않고 최종 실패로 기록하며, 반환값은 없습니다. */
    @Async("documentAnalysisExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void requestAnalysis(DocumentUploadedEvent event) {
        try {
            requestWithRetry(event);
        } catch (Exception e) {
            log.error("분석 시작 요청 처리 중 오류: documentId={}, error={}",
                    event.documentId(), e.getClass().getSimpleName());
            recordUnexpectedFailure(event);
        }
    }

    /** 문서가 PENDING으로 남지 않도록 AI_UNAVAILABLE 실패로 기록합니다. 기록까지 실패하면 로그만 남깁니다. */
    private void recordUnexpectedFailure(DocumentUploadedEvent event) {
        try {
            states.changeToFailed(event.documentId(), event.analysisId(), AnalysisFailureType.AI_UNAVAILABLE);
        } catch (Exception e) {
            log.error("분석 실패 기록 중 오류: documentId={}, error={}",
                    event.documentId(), e.getClass().getSimpleName());
        }
    }

    private static DocumentAnalysisRequest toRequest(MedicalDocument document) {
        return new DocumentAnalysisRequest(
                document.getId(), document.getStorageKey(), document.getMimeType(), document.getRequestId());
    }

    /**
     * 성공하거나 최종 실패할 때까지 최대 maxAttempts번 요청합니다.
     * 매 시도 직전에 문서를 다시 읽어, 없거나 UPLOADED가 아니면(삭제 등) 요청하지 않고 끝냅니다.
     */
    private void requestWithRetry(DocumentUploadedEvent event) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Optional<MedicalDocument> document = findUploadedDocument(event.documentId());
            if (document.isEmpty()) {
                return;
            }
            DocumentAnalysisResult result = analysisPort.requestAnalysis(toRequest(document.get()));
            if (result.isSuccess()) {
                return;
            }
            AnalysisFailureType failure = result.failureType();
            if (!failure.isRetryable() || attempt == maxAttempts) {
                states.changeToFailed(event.documentId(), event.analysisId(), failure);
                return;
            }
            states.updateRetryCount(event.analysisId());
            waitBeforeRetry(attempt);
        }
    }

    private Optional<MedicalDocument> findUploadedDocument(UUID documentId) {
        return documents.findById(documentId)
                .filter(document -> document.getStatus() == DocumentStatus.UPLOADED);
    }

    /** 시도 횟수에 비례해 기다립니다(기본 0.5초, 1초). */
    private void waitBeforeRetry(int attempt) {
        long millis = retryBackoff.toMillis() * attempt;
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("재시도 대기 중 중단되었습니다.", e);
        }
    }
}
