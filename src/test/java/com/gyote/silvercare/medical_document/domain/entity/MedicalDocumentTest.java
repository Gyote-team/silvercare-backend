package com.gyote.silvercare.medical_document.domain.entity;

import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.global.type.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalDocumentTest {

    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final String STORAGE_KEY = "documents/" + DOCUMENT_ID + "/original.pdf";
    private static final String CONTENT_SHA256 = "a".repeat(64);

    @Test
    void uploadedWithIdKeepsGivenIdAndUploadFields() {
        MedicalDocument document = uploadedDocument();

        assertThat(document.getId()).isEqualTo(DOCUMENT_ID);
        assertThat(document.getStorageKey()).isEqualTo(STORAGE_KEY);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(document.getDocumentType()).isEqualTo(DocumentType.UNKNOWN);
        assertThat(document.getDeclaredDocType()).isEqualTo(DocumentType.LAB_RESULT);
        assertThat(document.getIdempotencyKey()).isEqualTo("idem-key-1");
    }

    @Test
    void uploadedWithIdKeepsContentSha256() {
        assertThat(uploadedDocument().getContentSha256()).isEqualTo(CONTENT_SHA256);
    }

    private MedicalDocument uploadedDocument() {
        return MedicalDocument.uploadedWithId(
                DOCUMENT_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "검사결과.pdf", STORAGE_KEY, "application/pdf", 1024L,
                "request-1", "idem-key-1", DocumentType.LAB_RESULT, CONTENT_SHA256);
    }
}
