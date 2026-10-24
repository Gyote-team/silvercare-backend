package com.gyote.silvercare.medical_document.domain;

public enum DocumentAnalysisStatus {
    /** 문서 분석 작업이 생성되었지만 아직 시작되지 않은 상태입니다. */
    PENDING,
    /** 문서 분석 작업이 실행 중인 상태입니다. */
    RUNNING,
    /** 문서 분석이 정상적으로 완료된 상태입니다. */
    SUCCEEDED,
    /** 문서 분석이 오류로 실패한 상태입니다. */
    FAILED
}
