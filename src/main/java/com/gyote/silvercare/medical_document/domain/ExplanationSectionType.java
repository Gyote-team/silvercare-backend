package com.gyote.silvercare.medical_document.domain;

public enum ExplanationSectionType {
    /** 검사 결과의 수치와 의미를 설명하는 섹션입니다. */
    LAB_RESULT,
    /** 복용 약물과 복약 방법을 설명하는 섹션입니다. */
    MEDICATION,
    /** 추후 진료나 관리가 필요한 내용을 설명하는 섹션입니다. */
    FOLLOW_UP,
    /** 검사 또는 진료 예정 시점을 설명하는 섹션입니다. */
    TEST_SCHEDULE,
    /** 주의해야 할 위험이나 생활 관리 내용을 설명하는 섹션입니다. */
    CAUTION
}
