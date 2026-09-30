package com.gyote.silvercare.medical_document.query.model;

import java.time.LocalDate;
public interface AiDocumentListRow {

    String getDocumentId();

    String getDocumentName();

    String getDocumentType();

    String getVisitId();

    LocalDate getVisitedOn();

    String getCreatedAt();

    String getAuthorName();

    String getDocumentStatus();

    String getJobStatus();

    String getResultStatus();
}
