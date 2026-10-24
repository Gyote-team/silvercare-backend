package com.gyote.silvercare.medical_document.domain;

/** AI 서버에 문서 분석 시작을 요청하는 포트입니다. */
public interface DocumentAnalysisPort {

    /** 분석 시작을 요청하고 성공 또는 실패 유형을 담은 결과를 반환합니다. 실패해도 예외를 던지지 않습니다. */
    DocumentAnalysisResult requestAnalysis(DocumentAnalysisRequest request);
}
