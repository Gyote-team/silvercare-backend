package com.gyote.silvercare.global.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeContractTest {

    @Test
    void sharedTypesMatchTheAgreedContract() {
        assertThat(DocumentType.values()).containsExactly(
                DocumentType.LAB_RESULT,
                DocumentType.PRESCRIPTION,
                DocumentType.DIAGNOSIS,
                DocumentType.DISCHARGE_GUIDE,
                DocumentType.UNKNOWN
        );
        assertThat(ActionItemType.values()).containsExactly(
                ActionItemType.MEDICATION,
                ActionItemType.REVISIT,
                ActionItemType.TEST,
                ActionItemType.CAUTION,
                ActionItemType.OTHER
        );
    }
}
