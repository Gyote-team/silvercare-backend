package com.gyote.silvercare.medical_document.infrastructure;

import com.gyote.silvercare.medical_document.domain.DocumentAnalysisPort;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisRequest;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI 서버 주소가 설정되지 않았을 때 쓰는 구현입니다(AI 서버가 없는 로컬 개발용).
 * 실제로 요청하지 않으므로 분석 작업은 PENDING으로 남습니다.
 */
public class NoopDocumentAnalysisClient implements DocumentAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(NoopDocumentAnalysisClient.class);

    /** 요청을 보내지 않고 로그 한 줄만 남긴 뒤 성공 결과를 반환합니다. */
    @Override
    public DocumentAnalysisResult requestAnalysis(DocumentAnalysisRequest request) {
        log.info("AI 서버 주소가 없어 분석 시작 요청을 건너뜁니다: documentId={}, requestId={}",
                request.documentId(), request.requestId());
        return DocumentAnalysisResult.success();
    }
}
