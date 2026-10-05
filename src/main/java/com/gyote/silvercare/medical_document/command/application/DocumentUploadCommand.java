package com.gyote.silvercare.medical_document.command.application;

import java.util.UUID;

/** 문서 업로드 서비스 입력입니다. 컨트롤러가 요청에서 꺼낸 값과 파일 바이트만 담습니다. */
public record DocumentUploadCommand(
        UUID visitId,
        String idempotencyKey,
        String declaredDocType,
        String fileName,
        byte[] content
) {
}
