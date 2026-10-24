package com.gyote.silvercare.global.type;

/**
 * VLM·분류 엔진이 판별한 의료 문서 유형입니다.
 * FC-08-01-01 결과를 FC-06 조회 API에서 그대로 노출합니다.
 * 신뢰도가 낮으면 {@link #UNKNOWN}으로 두고 파이프라인은 계속 진행합니다.
 */
public enum DocumentType {
    /** 혈액검사·소변검사 등 검사 결과 문서입니다. */
    LAB_RESULT,
    /** 복약 또는 처방전 문서입니다. */
    PRESCRIPTION,
    /** 진단서 또는 진단 결과 문서입니다. */
    DIAGNOSIS,
    /** 퇴원 안내 및 퇴원 후 관리 문서입니다. */
    DISCHARGE_GUIDE,
    /** 유형을 판별하지 못한 문서입니다. */
    UNKNOWN
}
