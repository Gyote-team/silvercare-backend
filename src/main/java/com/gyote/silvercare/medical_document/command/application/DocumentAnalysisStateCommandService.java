package com.gyote.silvercare.medical_document.command.application;

import com.gyote.silvercare.medical_document.domain.AnalysisFailureType;
import com.gyote.silvercare.medical_document.domain.repository.DocumentAnalysisRepository;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** 분석 시작 요청의 재시도 횟수와 최종 실패 상태를 각각 한 트랜잭션으로 기록하는 코드입니다. */
@Service
@RequiredArgsConstructor
public class DocumentAnalysisStateCommandService {

    static final String FAILED_STEP = "ANALYSIS_REQUEST";

    private final MedicalDocumentRepository documents;
    private final DocumentAnalysisRepository analyses;

    /** 분석 작업의 재시도 횟수를 1 늘립니다. 작업이 없으면 아무 일도 하지 않고, 반환값은 없습니다. */
    @Transactional
    public void updateRetryCount(UUID analysisId) {
        analyses.findById(analysisId).ifPresent(analysis -> analysis.recordRetry());
    }

    /** 분석 작업을 FAILED로 기록하고, 문서가 아직 UPLOADED일 때만 FAILED로 바꿉니다. 반환값은 없습니다. */
    @Transactional
    public void changeToFailed(UUID documentId, UUID analysisId, AnalysisFailureType failureType) {
        Instant now = Instant.now();
        analyses.findById(analysisId).ifPresent(analysis -> analysis.fail(
                failureType.name(), FAILED_STEP, failureType.getMessage(), failureType.isRetryable(), now));
        documents.updateStatusToFailedIfUploaded(documentId, now);
    }
}
