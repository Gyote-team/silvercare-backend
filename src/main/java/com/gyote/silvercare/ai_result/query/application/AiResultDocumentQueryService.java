package com.gyote.silvercare.ai_result.query.application;

import com.gyote.silvercare.ai_result.domain.entity.AiResultDocument;
import com.gyote.silvercare.ai_result.domain.repository.AiResultDocumentRepository;
import com.gyote.silvercare.ai_result.error.AiResultErrorCode;
import com.gyote.silvercare.ai_result.query.model.AiResultDocumentView;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.query.application.CareRelationPermissionService;
import com.gyote.silvercare.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 접근 권한을 검증해 저장된 AI 답변 문서를 조회한다. */
@Service
@RequiredArgsConstructor
public class AiResultDocumentQueryService {

    private final AiResultDocumentRepository resultDocuments;
    private final CareRelationPermissionService permissions;

    /** 접근 권한을 확인한 뒤 AI 답변 문서를 조회한다. */
    @Transactional(readOnly = true)
    public AiResultDocumentView get(User actor, UUID resultDocumentId) {
        AiResultDocument result = resultDocuments.findById(resultDocumentId)
                .orElseThrow(() -> new BusinessException(AiResultErrorCode.RESULT_NOT_FOUND));
        permissions.requireDocumentAccess(actor, result.getPatientId());
        return AiResultDocumentView.from(result);
    }
}
