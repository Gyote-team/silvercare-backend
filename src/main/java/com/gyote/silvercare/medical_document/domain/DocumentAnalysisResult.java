package com.gyote.silvercare.medical_document.domain;

/** 분석 시작 요청의 결과입니다. 성공이면 failureType이 null이고, 실패면 실패 유형을 가집니다. */
public record DocumentAnalysisResult(AnalysisFailureType failureType) {

    /** 요청이 받아들여졌다는 성공 결과를 반환합니다. */
    public static DocumentAnalysisResult success() {
        return new DocumentAnalysisResult(null);
    }

    /** 주어진 유형으로 실패했다는 결과를 반환합니다. */
    public static DocumentAnalysisResult failure(AnalysisFailureType failureType) {
        return new DocumentAnalysisResult(failureType);
    }

    /** 요청이 성공했으면 true를 반환합니다. */
    public boolean isSuccess() {
        return failureType == null;
    }
}
