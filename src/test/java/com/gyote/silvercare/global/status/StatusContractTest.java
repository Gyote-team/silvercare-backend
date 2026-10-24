package com.gyote.silvercare.global.status;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusContractTest {

    @Test
    void sharedStatusesMatchTheAgreedContract() {
        assertThat(DocumentStatus.values()).containsExactly(
                DocumentStatus.UPLOADED,
                DocumentStatus.PROCESSING,
                DocumentStatus.READY,
                DocumentStatus.NEEDS_REVIEW,
                DocumentStatus.FAILED,
                DocumentStatus.DELETED
        );
        assertThat(AiJobStatus.values()).containsExactly(
                AiJobStatus.QUEUED,
                AiJobStatus.RUNNING,
                AiJobStatus.SUCCEEDED,
                AiJobStatus.FAILED,
                AiJobStatus.CANCELED
        );
        assertThat(ResultStatus.values()).containsExactly(
                ResultStatus.COMPLETE,
                ResultStatus.PARTIAL,
                ResultStatus.INSUFFICIENT
        );
        assertThat(ActionItemStatus.values()).containsExactly(
                ActionItemStatus.PENDING,
                ActionItemStatus.APPROVED,
                ActionItemStatus.REJECTED,
                ActionItemStatus.CANCELED
        );
        assertThat(TranscriptionStatus.values()).containsExactly(
                TranscriptionStatus.UPLOADED,
                TranscriptionStatus.PROCESSING,
                TranscriptionStatus.DRAFT_READY,
                TranscriptionStatus.CONFIRMED,
                TranscriptionStatus.FAILED,
                TranscriptionStatus.CANCELED
        );
        assertThat(OcrPageStatus.values()).containsExactly(
                OcrPageStatus.PENDING,
                OcrPageStatus.PROCESSING,
                OcrPageStatus.SUCCEEDED,
                OcrPageStatus.FAILED,
                OcrPageStatus.SKIPPED
        );
    }
}
