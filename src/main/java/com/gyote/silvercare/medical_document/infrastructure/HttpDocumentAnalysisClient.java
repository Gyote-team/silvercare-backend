package com.gyote.silvercare.medical_document.infrastructure;

import com.gyote.silvercare.medical_document.domain.AnalysisFailureType;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisPort;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisRequest;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

/**
 * AI 서버의 내부 API를 HTTP로 호출해 분석 시작을 요청하는 구현입니다.
 * 응답 코드와 연결 오류를 AnalysisFailureType으로 분류해 결과로 돌려줍니다.
 */
public class HttpDocumentAnalysisClient implements DocumentAnalysisPort {

    static final String TOKEN_HEADER = "X-Internal-Token";

    private static final Logger log = LoggerFactory.getLogger(HttpDocumentAnalysisClient.class);
    private static final String ANALYZE_PATH = "/internal/documents/{documentId}/analyze";

    private final RestClient restClient;

    /** timeout은 builder에 미리 설정된 request factory를 따르고, 여기서는 기본 주소와 내부 인증 헤더만 붙입니다. */
    public HttpDocumentAnalysisClient(RestClient.Builder builder, String baseUrl, String internalToken) {
        this.restClient = builder
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .defaultHeader(TOKEN_HEADER, internalToken)
                .build();
    }

    /** AI 서버에 분석 시작을 POST로 요청하고, 2xx면 성공을 아니면 분류한 실패 유형을 반환합니다. */
    @Override
    public DocumentAnalysisResult requestAnalysis(DocumentAnalysisRequest request) {
        try {
            HttpStatusCode status = restClient.post()
                    .uri(ANALYZE_PATH, request.documentId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            // 3xx는 예외 없이 돌아오므로 2xx가 아니면 여기서 실패로 분류합니다.
            if (!status.is2xxSuccessful()) {
                return failure(request, classifyStatus(status), status.value());
            }
            return DocumentAnalysisResult.success();
        } catch (RestClientResponseException e) {
            return failure(request, classifyStatus(e.getStatusCode()), e.getStatusCode().value());
        } catch (RestClientException e) {
            return failure(request, classifyError(e), null);
        }
    }

    private static AnalysisFailureType classifyStatus(HttpStatusCode status) {
        int code = status.value();
        if (code == 401 || code == 403) {
            return AnalysisFailureType.INTERNAL_AUTH_FAILED;
        }
        if (code == 408) {
            return AnalysisFailureType.AI_TIMEOUT;
        }
        if (code == 429 || status.is5xxServerError()) {
            return AnalysisFailureType.AI_UNAVAILABLE;
        }
        return AnalysisFailureType.AI_REQUEST_REJECTED;
    }

    private static AnalysisFailureType classifyError(RestClientException e) {
        return hasTimeoutCause(e) ? AnalysisFailureType.AI_TIMEOUT : AnalysisFailureType.AI_UNAVAILABLE;
    }

    private static boolean hasTimeoutCause(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }

    /** 토큰·파일명·환자 정보는 남기지 않고 documentId, requestId, 상태 코드, 실패 유형만 로그로 남깁니다. */
    private static DocumentAnalysisResult failure(
            DocumentAnalysisRequest request, AnalysisFailureType type, Integer statusCode) {
        log.warn("분석 시작 요청 실패: documentId={}, requestId={}, status={}, failureType={}",
                request.documentId(), request.requestId(), statusCode, type);
        return DocumentAnalysisResult.failure(type);
    }
}
