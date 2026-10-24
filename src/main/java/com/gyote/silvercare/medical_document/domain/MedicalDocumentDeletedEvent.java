package com.gyote.silvercare.medical_document.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 의료 문서가 소프트 삭제된 뒤 발행되는 이벤트입니다.
 * action_item 쪽이 이 이벤트를 받아 해당 문서에서 나온 PENDING 후보를 CANCELED 처리합니다(FC-13-00-06).
 */
public record MedicalDocumentDeletedEvent(
        UUID documentId,
        UUID patientId,
        UUID deletedBy,
        Instant deletedAt
) {
}
