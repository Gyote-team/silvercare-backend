package com.gyote.silvercare.medical_document.command.application;

import com.gyote.silvercare.medical_document.domain.DocumentUploadedEvent;
import com.gyote.silvercare.medical_document.domain.entity.DocumentAnalysis;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.repository.DocumentAnalysisRepository;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 업로드한 문서와 대기 중인 분석 작업을 한 트랜잭션으로 저장하고 업로드 이벤트를 발행하는 코드입니다. */
@Service
public class DocumentUploadSaveCommandService {

    private final MedicalDocumentRepository documents;
    private final DocumentAnalysisRepository analyses;
    private final ApplicationEventPublisher events;
    private final String parserVersion;

    public DocumentUploadSaveCommandService(
            MedicalDocumentRepository documents,
            DocumentAnalysisRepository analyses,
            ApplicationEventPublisher events,
            @Value("${silvercare.ai.parser-version:v1}") String parserVersion
    ) {
        this.documents = documents;
        this.analyses = analyses;
        this.events = events;
        this.parserVersion = parserVersion;
    }

    /** 문서와 PENDING 분석 작업을 저장하고 업로드 이벤트를 발행한 뒤, 저장된 문서를 반환합니다. */
    @Transactional
    public MedicalDocument registerUpload(MedicalDocument document) {
        MedicalDocument saved = documents.saveAndFlush(document);
        DocumentAnalysis analysis = analyses.save(DocumentAnalysis.queued(saved.getId(), parserVersion));
        events.publishEvent(new DocumentUploadedEvent(saved.getId(), analysis.getId()));
        return saved;
    }
}
