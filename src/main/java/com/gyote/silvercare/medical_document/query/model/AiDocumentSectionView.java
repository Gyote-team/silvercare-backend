package com.gyote.silvercare.medical_document.query.model;

import com.gyote.silvercare.medical_document.domain.ExplanationSectionType;

import java.util.List;
import java.util.UUID;

public record AiDocumentSectionView(
        UUID sectionId,
        ExplanationSectionType sectionType,
        String title,
        List<AiDocumentSectionItemView> items
) {
}
