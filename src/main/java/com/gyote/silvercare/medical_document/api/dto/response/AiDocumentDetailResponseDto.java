package com.gyote.silvercare.medical_document.api.dto.response;

import java.time.Instant;

public record AiDocumentDetailResponseDto(
        // 문서의 고유 식별자입니다. 예: "doc-001"
        String documentId,
        // 원본 의료 문서 이름입니다. 예: "2025년 9월 건강검진 결과.pdf"
        String documentName,
        // 의료 문서 유형입니다. 예: "EXAM_RESULT"
        String documentType,
        // 문서가 생성된 방문의 ID입니다. 예: "visit-001"
        String visitId,
        // 원본 문서 처리 상태입니다. 예: "COMPLETED"
        String documentStatus,
        // AI 분석 작업 상태입니다. 예: "SUCCEEDED"
        String jobStatus,
        // AI 설명 결과 상태입니다. 예: "COMPLETE"
        String resultStatus,
        // 생성된 쉬운 설명의 제목입니다. 예: "건강검진 결과 요약"
        String title,
        // 보호자도 이해할 수 있도록 작성된 설명 본문입니다. 예: "혈압 수치는 정상 범위입니다."
        String content,
        // 문서가 등록된 시각입니다. 예: 2025-09-30T09:30:00Z
        Instant createdAt,
        // 문서에 포함된 핵심정보 섹션 수입니다. 예: 5
        long sectionCount,
        // 설명에서 참조한 원문 근거 수입니다. 예: 3
        long citationCount
) {
}
