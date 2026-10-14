package com.gyote.silvercare.ai_result.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiResultWorkerConfig {

    @Bean
    @ConditionalOnProperty(prefix = "silvercare.ai", name = "base-url")
    AiResultWorkerClient aiResultWorkerClient(
            RestClient.Builder builder,
            @Value("${silvercare.ai.base-url:}") String baseUrl,
            @Value("${silvercare.ai.internal-token:}") String internalToken
    ) {
        return new AiResultWorkerClient(builder, baseUrl, internalToken);
    }
}
