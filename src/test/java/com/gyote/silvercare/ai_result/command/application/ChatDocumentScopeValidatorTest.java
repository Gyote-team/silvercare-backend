package com.gyote.silvercare.ai_result.command.application;

import com.gyote.silvercare.ai_chat.error.AiChatErrorCode;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class ChatDocumentScopeValidatorTest {

    private final MedicalDocumentRepository documents = mock(MedicalDocumentRepository.class);
    private final ChatDocumentScopeValidator validator = new ChatDocumentScopeValidator(documents);

    @Test
    void acceptsAllSelectedDocumentsWhenAtLeastOneIsReady() {
        UUID patientId = UUID.randomUUID();
        MedicalDocument ready = document(patientId, DocumentStatus.READY);
        MedicalDocument processing = document(patientId, DocumentStatus.PROCESSING);
        List<UUID> ids = List.of(ready.getId(), processing.getId());
        when(documents.findAllById(any())).thenReturn(List.of(ready, processing));

        assertThatCode(() -> validator.validate(patientId, ids)).doesNotThrowAnyException();
    }

    @Test
    void rejectsWholeRequestWhenOneDocumentIsOutsidePatientScope() {
        UUID patientId = UUID.randomUUID();
        MedicalDocument allowed = document(patientId, DocumentStatus.READY);
        UUID missing = UUID.randomUUID();
        List<UUID> ids = List.of(allowed.getId(), missing);
        when(documents.findAllById(any())).thenReturn(List.of(allowed));

        assertThatThrownBy(() -> validator.validate(patientId, ids))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        org.assertj.core.api.Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(AiChatErrorCode.DOCUMENT_SCOPE_DENIED));
    }

    @Test
    void rejectsWhenEverySelectedDocumentIsStillProcessing() {
        UUID patientId = UUID.randomUUID();
        MedicalDocument processing = document(patientId, DocumentStatus.PROCESSING);
        List<UUID> ids = List.of(processing.getId());
        when(documents.findAllById(any())).thenReturn(List.of(processing));

        assertThatThrownBy(() -> validator.validate(patientId, ids))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        org.assertj.core.api.Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(AiChatErrorCode.DOCUMENT_NOT_READY));
    }

    private MedicalDocument document(UUID patientId, DocumentStatus status) {
        MedicalDocument document = mock(MedicalDocument.class);
        when(document.getId()).thenReturn(UUID.randomUUID());
        when(document.getPatientId()).thenReturn(patientId);
        when(document.getStatus()).thenReturn(status);
        when(document.isDeleted()).thenReturn(false);
        when(document.getDeletedAt()).thenReturn(null);
        return document;
    }
}
