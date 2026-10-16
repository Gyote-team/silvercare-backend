package com.gyote.silvercare.ai_result.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyote.silvercare.ai_result.domain.QuestionType;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class AiResultGenerationClientConfigTest {
    @Test
    void stopsWaitingWhenAiServerDoesNotRespond() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/ai-result-documents/generate", exchange -> {
            try {
                release.await(5, TimeUnit.SECONDS);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            AiResultGenerationClient client = new AiResultGenerationClientConfig()
                    .aiResultGenerationClient(RestClient.builder(), baseUrl, "test-token",
                            Duration.ofSeconds(1), Duration.ofMillis(100));
            AiResultGenerationClient.GenerationRequest request =
                    new AiResultGenerationClient.GenerationRequest(UUID.randomUUID(), UUID.randomUUID(),
                            UUID.randomUUID(), "검사 결과를 알려줘", QuestionType.DOCUMENT_EASY_EXPLANATION,
                            List.of());

            assertThatThrownBy(() -> client.generate(request))
                    .isInstanceOf(ResourceAccessException.class);
        } finally {
            release.countDown();
            server.stop(0);
        }
    }
}
