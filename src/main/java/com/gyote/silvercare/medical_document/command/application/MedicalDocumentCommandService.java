package com.gyote.silvercare.medical_document.command.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.MedicalDocumentAccessPolicy;
import com.gyote.silvercare.medical_document.domain.MedicalDocumentDeletedEvent;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import com.gyote.silvercare.user.domain.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** State-changing medical-document use cases only. */
@Service
public class MedicalDocumentCommandService {

    private final MedicalDocumentRepository documents;
    private final MedicalDocumentAccessPolicy accessPolicy;
    private final ApplicationEventPublisher events;

    public MedicalDocumentCommandService(
            MedicalDocumentRepository documents,
            MedicalDocumentAccessPolicy accessPolicy,
            ApplicationEventPublisher events
    ) {
        this.documents = documents;
        this.accessPolicy = accessPolicy;
        this.events = events;
    }

    @Transactional
    public void delete(User me, UUID documentId) {
        MedicalDocument document = documents.findByIdAndDocumentStatusNot(documentId, DocumentStatus.DELETED)
                .orElseThrow(() -> new BusinessException(MedicalDocumentErrorCode.DOCUMENT_NOT_FOUND));
        accessPolicy.checkDeletable(me, document);
        Instant now = Instant.now();
        document.delete(now);
        events.publishEvent(new MedicalDocumentDeletedEvent(
                document.getId(), document.getPatientId(), me.getId(), now));
    }
}
