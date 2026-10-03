package com.gyote.silvercare.medical_document.infrastructure;

import com.gyote.silvercare.medical_document.domain.DocumentAnalysisPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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

    /** AI 서버 주소가 비어 있으면 No-op 구현을, 있으면 timeout을 설정한 HTTP 구현을 반환합니다. */
    @Bean
    public DocumentAnalysisPort documentAnalysisPort(
            RestClient.Builder builder,
            @Value("${silvercare.ai.base-url:}") String baseUrl,
            @Value("${silvercare.ai.internal-token:}") String internalToken,
            @Value("${silvercare.ai.connect-timeout:2s}") Duration connectTimeout,
            @Value("${silvercare.ai.read-timeout:5s}") Duration readTimeout
    ) {
        if (!StringUtils.hasText(baseUrl)) {
            return new NoopDocumentAnalysisClient();
        }
        builder.requestFactory(requestFactory(connectTimeout, readTimeout));
        return new HttpDocumentAnalysisClient(builder, baseUrl, internalToken);
    }

    /** 분석 요청 리스너가 쓰는 스레드풀(core 2, max 4, queue 100)을 반환합니다. */
    @Bean
    public ThreadPoolTaskExecutor documentAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doc-analysis-");
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
