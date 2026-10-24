package com.gyote.silvercare.global.status;

/** AI가 만든 결과의 완전성 상태입니다. */
public enum ResultStatus {
    /** AI 결과가 충분한 근거와 내용을 갖춘 완전한 상태입니다. */
    COMPLETE,
    /** 일부 내용만 생성되었거나 일부 근거가 부족한 상태입니다. */
    PARTIAL,
    /** AI가 충분한 내용을 생성하지 못한 상태입니다. */
    INSUFFICIENT
}
