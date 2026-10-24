package com.gyote.silvercare.medical_document.query.model;

public interface AiDocumentDetailRow {

    String getDocumentId();

    String getDocumentName();

    String getDocumentType();

    String getVisitId();

    String getDocumentStatus();

    String getAnalysisStatus();

    String getJobStatus();

    String getResultStatus();

    String getTitle();

    String getContent();

    String getCreatedAt();

    Long getSectionCount();

    Long getCitationCount();
}
