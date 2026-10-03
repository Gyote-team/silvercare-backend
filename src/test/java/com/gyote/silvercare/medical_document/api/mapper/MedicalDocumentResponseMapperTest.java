package com.gyote.silvercare.medical_document.api.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyote.silvercare.global.status.AiJobStatus;
import com.gyote.silvercare.medical_document.command.application.DocumentUploadResult;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 업로드 응답 JSON이 공개 필드 6개만 담고 저장소 경로를 노출하지 않는지 확인하는 테스트입니다. */
class MedicalDocumentResponseMapperTest {

    private final MedicalDocumentResponseMapper mapper = new MedicalDocumentResponseMapper();

    @Test
    void uploadResponseHasExactlySixPublicFields() {
        var response = mapper.toUploadResponse(new DocumentUploadResult(document(), AiJobStatus.QUEUED, true));

        Map<String, Object> json = new ObjectMapper().convertValue(response, new TypeReference<>() {
        });

        assertThat(json.keySet()).containsExactlyInAnyOrder(
                "documentId", "visitId", "mimeType", "documentStatus", "latestAiJobStatus", "requestId");
        assertThat(json.keySet()).doesNotContain("objectKey", "storageKey");
    }

    private static MedicalDocument document() {
        return MedicalDocument.uploadedWithId(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "검사결과.pdf", "documents/test/original.pdf", "application/pdf", 10L,
                "upload-20261003-abcd1234", "key-1", null);
    }
}
