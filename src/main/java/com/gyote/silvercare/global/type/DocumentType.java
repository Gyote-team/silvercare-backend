package com.gyote.silvercare.global.type;

/**
 * VLM·분류 엔진이 판별한 의료 문서 유형입니다.
 * FC-08-01-01 결과를 FC-06 조회 API에서 그대로 노출합니다.
 * 신뢰도가 낮으면 {@link #UNKNOWN}으로 두고 파이프라인은 계속 진행합니다.
 */
public enum DocumentType {
    LAB_RESULT,
    PRESCRIPTION,
    DIAGNOSIS,
    DISCHARGE_GUIDE,
    UNKNOWN
}
