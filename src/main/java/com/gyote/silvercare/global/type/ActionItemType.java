package com.gyote.silvercare.global.type;

/**
 * AI가 추출한 할 일 후보 종류입니다.
 * FC-13에서 생성되고 FC-07·#19에서 승인·일정 처리 시 사용합니다.
 * 복약 알림(#20)은 {@link #MEDICATION}만 대상으로 합니다.
 */
public enum ActionItemType {
    MEDICATION,
    REVISIT,
    TEST,
    CAUTION,
    OTHER
}
