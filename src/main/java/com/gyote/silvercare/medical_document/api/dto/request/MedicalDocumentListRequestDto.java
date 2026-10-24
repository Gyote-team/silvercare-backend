package com.gyote.silvercare.medical_document.api.dto.request;

import java.util.UUID;

/** 의료 문서 목록 조회 쿼리 파라미터. 모두 선택값입니다. */
public record MedicalDocumentListRequestDto(
        UUID patientId,
        UUID visitId,
        String cursor,
        Integer size
) {
}
