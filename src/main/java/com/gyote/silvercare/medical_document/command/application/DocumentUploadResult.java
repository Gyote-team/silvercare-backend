package com.gyote.silvercare.medical_document.command.application;

import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;

/** 문서 업로드 결과입니다. created가 false면 같은 Idempotency-Key 재요청이라 기존 문서를 돌려준 것입니다. */
public record DocumentUploadResult(
        MedicalDocument document,
        AiJobStatus latestAiJobStatus,
        boolean created
) {
}
