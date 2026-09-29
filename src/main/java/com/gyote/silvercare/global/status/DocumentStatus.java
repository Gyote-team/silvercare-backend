package com.gyote.silvercare.global.status;

/** 문서 저장·AI 분석 결과의 문서 단위 상태입니다. */
public enum DocumentStatus {
    /** 문서가 저장되었지만 아직 처리가 시작되지 않은 상태입니다. */
    UPLOADED,
    /** 문서 저장 후 파싱·분석 등 처리가 진행 중인 상태입니다. */
    PROCESSING,
    /** 문서 처리와 검토가 완료되어 조회 가능한 상태입니다. */
    READY,
    /** 자동 처리 결과에 추가 확인이 필요한 상태입니다. */
    NEEDS_REVIEW,
    /** 문서 처리 중 오류가 발생한 상태입니다. */
    FAILED,
    /** 사용자가 삭제했거나 논리 삭제된 상태입니다. */
    DELETED
}
