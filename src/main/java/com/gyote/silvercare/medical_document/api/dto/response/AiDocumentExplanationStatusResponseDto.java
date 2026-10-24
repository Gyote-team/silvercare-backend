package com.gyote.silvercare.medical_document.api.dto.response;

import java.time.Instant;

public record AiDocumentExplanationStatusResponseDto(
        // 상태를 조회한 문서의 ID입니다. 예: "doc-001"
        String documentId,
        // AI 설명 생성 작업 상태입니다. 예: "RUNNING"
        String jobStatus,
        // 현재까지 생성된 결과의 상태입니다. 예: "PARTIAL"
        String resultStatus,
        // 현재 처리 중인 단계입니다. 예: "GENERATING_EXPLANATION"
        String currentStep,
        // 전체 처리 진행률(0~100)입니다. 예: 75
        Integer progress,
        // 설명 생성이 완료된 시각입니다. 미완료이면 null일 수 있습니다. 예: 2025-09-30T10:00:00Z
        Instant completedAt,
        // 설명 결과를 확인할 수 있는 경로입니다. 예: "/api/ai-documents/doc-001"
        String detailUrl,
        // 실패가 발생한 처리 단계입니다. 실패가 아니면 null입니다. 예: "OCR"
        String failedStep,
        // 실패 원인 코드입니다. 실패가 아니면 null입니다. 예: "AI_TIMEOUT"
        String errorCode,
        // 동일 작업을 다시 요청할 수 있는지 여부입니다. 예: true
        boolean retryable,
        // 분석 대상이 된 원본 문서의 경로입니다. 예: "s3://silvercare/documents/doc-001.pdf"
        String originalDocumentUrl
) {
}
