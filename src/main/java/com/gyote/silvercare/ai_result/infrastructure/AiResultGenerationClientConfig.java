package com.gyote.silvercare.ai_result.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AiResultGenerationClientConfig {

    /** 설정된 AI Backend 주소와 내부 토큰으로 연동 클라이언트를 등록한다. */
    @Bean
    @ConditionalOnProperty(prefix = "silvercare.ai", name = "base-url")
    AiResultGenerationClient aiResultGenerationClient(
            RestClient.Builder builder,
            @Value("${silvercare.ai.base-url:}") String baseUrl,
            @Value("${silvercare.ai.internal-token:}") String internalToken,
            @Value("${silvercare.ai.connect-timeout:2s}") Duration connectTimeout,
            @Value("${silvercare.ai.result-read-timeout:330s}") Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        builder.requestFactory(requestFactory);
        return new AiResultGenerationClient(builder, baseUrl, internalToken);
    }
}
