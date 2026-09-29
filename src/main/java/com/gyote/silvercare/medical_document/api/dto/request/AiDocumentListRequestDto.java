package com.gyote.silvercare.medical_document.api.dto.request;

public record AiDocumentListRequestDto(
        // 조회할 환자 ID입니다. 예: "patient-001"
        String patientId,
        // 특정 방문으로 조회 범위를 좁힐 때 사용하는 방문 ID입니다. 예: "visit-20250930-001"
        String visitId,
        // 문서 유형 필터입니다. 예: "PRESCRIPTION", "EXAM_RESULT"
        String docType,
        // 문서 상태 필터입니다. 예: "COMPLETED"
        String status,
        // 조회할 페이지 번호입니다. 예: 0 (첫 페이지)
        int page,
        // 한 페이지에 조회할 문서 개수입니다. 예: 20
        int size
) {
}
