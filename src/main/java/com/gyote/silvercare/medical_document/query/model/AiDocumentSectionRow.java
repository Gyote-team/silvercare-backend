package com.gyote.silvercare.medical_document.query.model;

public interface AiDocumentSectionRow {

    String getDocumentId();

    String getSectionId();

    String getSectionType();

    String getSectionTitle();

    Integer getSectionOrder();

    String getSentenceId();

    String getSourceItemId();

    String getLabel();

    String getDisplayValue();

    String getUnit();

    Boolean getHasSource();

    String getCitationId();
}
