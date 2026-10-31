package com.gyote.silvercare.ai_result.domain;

public enum QuestionType {
    VISIT_CHANGE_REPORT,
    TEST_RESULT_COMPARISON,
    DOCUMENT_EASY_EXPLANATION,
    LAB_RESULT_COMPARISON,
    LAB_REFERENCE_RANGE,
    MEDICATION_SUMMARY,
    FOLLOW_UP_SCHEDULE,
    VISIT_CHECKLIST,
    SOURCE_LOOKUP,
    MEDICAL_DECISION_REQUEST,
    UNSUPPORTED;

    public QuestionType canonical() {
        return this == LAB_RESULT_COMPARISON ? TEST_RESULT_COMPARISON : this;
    }
}
