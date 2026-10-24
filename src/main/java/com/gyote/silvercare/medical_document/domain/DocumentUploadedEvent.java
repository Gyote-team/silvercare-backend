package com.gyote.silvercare.medical_document.domain;

import java.util.UUID;

/**
 * 의료 문서 업로드가 저장된 뒤 발행되는 이벤트입니다.
 * AI 분석 요청 쪽이 이 이벤트를 받아 analysisId 작업으로 분석 서버를 호출합니다.
 */
public record DocumentUploadedEvent(
        UUID documentId,
        UUID analysisId
) {
}
