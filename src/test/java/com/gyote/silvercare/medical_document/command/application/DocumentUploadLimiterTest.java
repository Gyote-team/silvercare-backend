package com.gyote.silvercare.medical_document.command.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 업로드 리미터가 한도를 넘는 호출을 거절하고, 작업이 끝나면 자리를 돌려주는지 확인하는 테스트입니다. */
class DocumentUploadLimiterTest {

    private final DocumentUploadLimiter limiter = new DocumentUploadLimiter(1);

    @Test
    void secondCallWhileRunningIsRejected() {
        // Semaphore는 같은 스레드의 재진입도 막으므로, 실행 중에 한 번 더 부르면 자리가 없는 상태가 됩니다.
        assertThatThrownBy(() -> limiter.run(() -> limiter.run(() -> "inner")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MedicalDocumentErrorCode.TOO_MANY_UPLOADS));
    }

    @Test
    void permitIsReturnedAfterSuccess() {
        limiter.run(() -> "first");

        assertThat(limiter.run(() -> "second")).isEqualTo("second");
    }

    @Test
    void permitIsReturnedAfterException() {
        assertThatThrownBy(() -> limiter.run(() -> {
            throw new IllegalStateException("작업 실패");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(limiter.run(() -> "next")).isEqualTo("next");
    }

    @Test
    void limitBelowOneFailsAtStartup() {
        assertThatThrownBy(() -> new DocumentUploadLimiter(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
