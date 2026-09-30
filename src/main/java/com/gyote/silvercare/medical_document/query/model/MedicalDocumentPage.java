package com.gyote.silvercare.medical_document.query.model;

import java.util.List;

/** 의료 문서 목록 한 페이지입니다. 다음 페이지가 없으면 nextCursor는 null입니다. */
public record MedicalDocumentPage(
        List<MedicalDocumentView> items,
        String nextCursor
) {
}
