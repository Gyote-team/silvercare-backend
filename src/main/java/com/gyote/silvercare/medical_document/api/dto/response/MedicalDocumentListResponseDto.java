package com.gyote.silvercare.medical_document.api.dto.response;

import java.util.List;

/** 의료 문서 목록 응답. nextCursor를 다음 요청의 cursor로 넘기면 이어서 조회합니다. */
public record MedicalDocumentListResponseDto(
        List<MedicalDocumentDetailResponseDto> items,
        String nextCursor
) {
}
