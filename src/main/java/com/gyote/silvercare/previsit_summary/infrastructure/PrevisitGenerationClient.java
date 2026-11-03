package com.gyote.silvercare.previsit_summary.infrastructure;

import com.gyote.silvercare.previsit_summary.domain.PrevisitGenerationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;

/** 내부 인증과 제한 시간을 설정한 진료 전 요약 전용 AI 클라이언트. */
@Component
public class PrevisitGenerationClient implements PrevisitGenerationPort {
    private final RestClient client;
    private final boolean available;
    public PrevisitGenerationClient(RestClient.Builder builder,
            @Value("${silvercare.ai.base-url:}") String base,
            @Value("${silvercare.ai.internal-token:}") String token) {
        available=!base.isBlank() && !token.isBlank();
        var http=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3)).build();
        var factory=new JdkClientHttpRequestFactory(http); factory.setReadTimeout(Duration.ofSeconds(120));
        client=builder.baseUrl(base.isBlank()?"http://127.0.0.1:8000":base)
                .requestFactory(factory).defaultHeader("X-Internal-Token",token).build();
    }
    /** 기동 설정으로 AI 호출 가능 여부를 반환한다. */
    public boolean available(){return available;}
    /** 권한 확인을 마친 기록만 AI 서버에 전달한다. */
    public Result generate(Input input) {
        if(!available) throw new IllegalStateException("AI_NOT_CONFIGURED");
        return client.post().uri("/internal/previsit-summaries/generate").contentType(MediaType.APPLICATION_JSON)
                .body(input).retrieve().body(Result.class);
    }
}
