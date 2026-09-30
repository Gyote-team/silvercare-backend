package com.gyote.silvercare.action_item.domain;

/** 진료 문서에서 뽑아낸 할 일 후보의 종류입니다. */
public enum ActionItemType {
    /** 복약 관련 할 일입니다. */
    MEDICATION,
    /** 재방문·재진 관련 할 일입니다. */
    REVISIT,
    /** 검사 관련 할 일입니다. */
    TEST,
    /** 주의사항 관련 할 일입니다. */
    CAUTION,
    /** 그 밖의 할 일입니다. */
    OTHER
}
