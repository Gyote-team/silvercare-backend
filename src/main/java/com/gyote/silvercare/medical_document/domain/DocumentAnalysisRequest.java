package com.gyote.silvercare.medical_document.domain;

import java.util.UUID;

/**
 * AI 서버에 보내는 분석 시작 요청 값입니다. 이 4개 필드가 그대로 요청 body가 됩니다.
 * 환자 이름·연락처 같은 개인정보는 넣지 않습니다.
 */
public record DocumentAnalysisRequest(
        UUID documentId,
        String objectKey,
        String mimeType,
        String requestId
) {
}
