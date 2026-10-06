package com.gyote.silvercare.medical_document.infrastructure;

import com.gyote.silvercare.medical_document.domain.DocumentAnalysisPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/** AI 서버 주소·내부 토큰 설정에 따라 분석 요청 구현이 선택되거나 기동이 실패하는지 확인하는 테스트입니다. */
@ExtendWith(OutputCaptureExtension.class)
class DocumentAnalysisConfigTest {

    // 실제 앱처럼 "2s" 같은 설정값을 Duration으로 바꾸도록 Spring Boot 변환기를 넣습니다.
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(DocumentAnalysisConfig.class)
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void emptyBaseUrlSelectsNoop() {
        runner.run(context -> assertThat(context.getBean(DocumentAnalysisPort.class))
                .isInstanceOf(NoopDocumentAnalysisClient.class));
    }

    @Test
    void emptyBaseUrlLogsWarning(CapturedOutput output) {
        runner.run(context -> assertThat(output).contains("WARN").contains("silvercare.ai.base-url"));
    }

    @Test
    void configuredBaseUrlSelectsHttp() {
        runner.withPropertyValues("silvercare.ai.base-url=http://ai.test", "silvercare.ai.internal-token=test-token")
                .run(context -> assertThat(context.getBean(DocumentAnalysisPort.class))
                        .isInstanceOf(HttpDocumentAnalysisClient.class));
    }

    @Test
    void baseUrlWithoutInternalTokenFailsStartup() {
        runner.withPropertyValues("silvercare.ai.base-url=http://ai.test")
                .run(context -> assertThat(context.getStartupFailure())
                        .rootCause().hasMessageContaining("silvercare.ai.internal-token"));
    }
}
