package com.gyote.silvercare.medical_document.query.model;

public interface AiDocumentStatusRow {

    String getDocumentId();

    String getAnalysisStatus();

    String getJobStatus();

    String getResultStatus();

    String getCurrentStep();

    Integer getProgress();

    String getCompletedAt();

    String getFailedStep();

    String getErrorCode();

    Boolean getRetryable();
}
