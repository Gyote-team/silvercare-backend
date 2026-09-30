package com.gyote.silvercare.medical_document.api.dto.response;

import java.time.Instant;

public record AiDocumentListItemResponseDto(
        // 문서의 고유 식별자입니다. 예: "doc-001"
        String documentId,
        // 목록에 표시할 원본 문서 이름입니다. 예: "처방전.pdf"
        String documentName,
        // 의료 문서 유형입니다. 예: "PRESCRIPTION"
        String documentType,
        // 문서가 생성된 방문의 ID입니다. 예: "visit-001"
        String visitId,
        // 진료 또는 방문 날짜입니다. 예: "2025-09-30"
        String visitedOn,
        // 문서가 등록된 시각입니다. 예: 2025-09-30T09:30:00Z
        Instant createdAt,
        // 문서를 작성한 의료진 또는 기관 이름입니다. 예: "서울가정의학과"
        String authorName,
        // 원본 문서 처리 상태입니다. 예: "COMPLETED"
        String documentStatus,
        // AI 분석 작업 상태입니다. 예: "SUCCEEDED"
        String jobStatus,
        // AI 설명 결과 상태입니다. 예: "COMPLETE"
        String resultStatus
) {
}
