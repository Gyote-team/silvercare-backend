package com.gyote.silvercare.ai_result.command.application;

import com.gyote.silvercare.ai_chat.error.AiChatErrorCode;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.status.DocumentStatus;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** 채팅에서 선택한 문서 전체의 환자·삭제·분석 상태를 검증한다. */
@Component
@RequiredArgsConstructor
public class ChatDocumentScopeValidator {

    private final MedicalDocumentRepository documents;

    /** 일부 문서를 조용히 제외하지 않고 요청된 문서 전체를 검증한다. */
    public void validate(UUID patientId, List<UUID> documentIds) {
        Set<UUID> requested = Set.copyOf(documentIds);
        List<MedicalDocument> found = documents.findAllById(requested);
        Set<UUID> permitted = found.stream()
                .filter(document -> patientId.equals(document.getPatientId()))
                .filter(document -> !document.isDeleted() && document.getDeletedAt() == null)
                .map(MedicalDocument::getId)
                .collect(Collectors.toSet());
        if (!permitted.equals(requested)) {
            throw new BusinessException(AiChatErrorCode.DOCUMENT_SCOPE_DENIED);
        }
        boolean hasReadyDocument = found.stream().anyMatch(document ->
                document.getStatus() == DocumentStatus.READY
                        || document.getStatus() == DocumentStatus.NEEDS_REVIEW);
        if (!hasReadyDocument) {
            throw new BusinessException(AiChatErrorCode.DOCUMENT_NOT_READY);
        }
    }
}
