package com.gyote.silvercare.medical_document.infrastructure;

import com.gyote.silvercare.medical_document.domain.DocumentAnalysisPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 문서 분석 요청에 필요한 빈을 만드는 설정입니다.
 * AI 서버 주소 유무에 따라 분석 요청 구현을 고르고, 분석 요청 전용 비동기 스레드풀을 등록합니다.
 */
@Configuration
@EnableAsync
public class DocumentAnalysisConfig {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisConfig.class);

    /** AI 서버 주소가 비어 있으면 No-op 구현을, 있으면 timeout을 설정한 HTTP 구현을 반환합니다. 주소만 있고 내부 토큰이 비어 있으면 기동을 실패시킵니다. */
    @Bean
    public DocumentAnalysisPort documentAnalysisPort(
            RestClient.Builder builder,
            @Value("${silvercare.ai.base-url:}") String baseUrl,
            @Value("${silvercare.ai.internal-token:}") String internalToken,
            @Value("${silvercare.ai.connect-timeout:2s}") Duration connectTimeout,
            @Value("${silvercare.ai.read-timeout:5s}") Duration readTimeout
    ) {
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("AI 서버 주소(silvercare.ai.base-url)가 없어 분석 시작 요청을 보내지 않습니다.");
            return new NoopDocumentAnalysisClient();
        }
        Assert.state(StringUtils.hasText(internalToken),
                "silvercare.ai.base-url이 설정됐지만 silvercare.ai.internal-token이 비어 있습니다.");
        builder.requestFactory(requestFactory(connectTimeout, readTimeout));
        return new HttpDocumentAnalysisClient(builder, baseUrl, internalToken);
    }

    /** 분석 요청 리스너가 쓰는 스레드풀(core 2, max 4, queue 100)을 반환합니다. 종료 시 진행 중인 작업을 최대 30초 기다립니다. */
    @Bean
    public ThreadPoolTaskExecutor documentAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doc-analysis-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    // HttpURLConnection 기반 factory는 POST의 401 응답을 연결 오류로 던질 수 있어 JDK HttpClient를 씁니다.
    private static JdkClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
