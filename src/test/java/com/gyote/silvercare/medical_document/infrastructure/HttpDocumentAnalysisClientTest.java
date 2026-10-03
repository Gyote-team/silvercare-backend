package com.gyote.silvercare.medical_document.infrastructure;

import com.gyote.silvercare.medical_document.domain.AnalysisFailureType;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisRequest;
import com.gyote.silvercare.medical_document.domain.DocumentAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** HTTP 분석 요청 구현이 약속한 주소·헤더·body로 요청하고, 응답과 오류를 실패 유형으로 분류하는지 확인하는 테스트입니다. */
class HttpDocumentAnalysisClientTest {

    private static final UUID DOCUMENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String ANALYZE_URL = "http://ai.test/internal/documents/" + DOCUMENT_ID + "/analyze";
    private static final DocumentAnalysisRequest REQUEST = new DocumentAnalysisRequest(
            DOCUMENT_ID, "documents/" + DOCUMENT_ID + "/original.pdf", "application/pdf", "upload-20261003-abcd1234");

    private MockRestServiceServer server;
    private HttpDocumentAnalysisClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpDocumentAnalysisClient(builder, "http://ai.test/", "test-token");
    }

    @Test
    void postsToAnalyzeUrlWithInternalTokenAndExactlyFourBodyKeys() {
        expectAnalyze()
                .andExpect(header(HttpDocumentAnalysisClient.TOKEN_HEADER, "test-token"))
                .andExpect(content().json("""
                        {
                          "documentId": "11111111-2222-3333-4444-555555555555",
                          "objectKey": "documents/11111111-2222-3333-4444-555555555555/original.pdf",
                          "mimeType": "application/pdf",
                          "requestId": "upload-20261003-abcd1234"
                        }
                        """, true))
                .andRespond(withSuccess());

        DocumentAnalysisResult result = client.requestAnalysis(REQUEST);

        assertThat(result.isSuccess()).isTrue();
        server.verify();
    }

    @Test
    void serverErrorIsUnavailable() {
        assertFailure(withStatus(HttpStatus.INTERNAL_SERVER_ERROR), AnalysisFailureType.AI_UNAVAILABLE);
    }

    @Test
    void tooManyRequestsIsUnavailable() {
        assertFailure(withStatus(HttpStatus.TOO_MANY_REQUESTS), AnalysisFailureType.AI_UNAVAILABLE);
    }

    @Test
    void unauthorizedIsInternalAuthFailed() {
        assertFailure(withStatus(HttpStatus.UNAUTHORIZED), AnalysisFailureType.INTERNAL_AUTH_FAILED);
    }

    @Test
    void forbiddenIsInternalAuthFailed() {
        assertFailure(withStatus(HttpStatus.FORBIDDEN), AnalysisFailureType.INTERNAL_AUTH_FAILED);
    }

    @Test
    void badRequestIsRequestRejected() {
        assertFailure(withStatus(HttpStatus.BAD_REQUEST), AnalysisFailureType.AI_REQUEST_REJECTED);
    }

    @Test
    void connectionErrorIsUnavailable() {
        assertFailure(withException(new ConnectException("연결 거부")), AnalysisFailureType.AI_UNAVAILABLE);
    }

    @Test
    void socketTimeoutIsTimeout() {
        assertFailure(withException(new SocketTimeoutException("시간 초과")), AnalysisFailureType.AI_TIMEOUT);
    }

    private ResponseActions expectAnalyze() {
        return server.expect(requestTo(ANALYZE_URL)).andExpect(method(HttpMethod.POST));
    }

    /** AI 서버가 주어진 응답을 돌려줄 때 결과가 expected 실패 유형인지 확인합니다. */
    private void assertFailure(ResponseCreator response, AnalysisFailureType expected) {
        expectAnalyze().andRespond(response);

        DocumentAnalysisResult result = client.requestAnalysis(REQUEST);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.failureType()).isEqualTo(expected);
    }
}
