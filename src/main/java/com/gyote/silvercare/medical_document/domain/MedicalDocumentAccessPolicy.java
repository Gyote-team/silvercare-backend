package com.gyote.silvercare.medical_document.domain;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import com.gyote.silvercare.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** 의료 문서 열람 권한 규칙입니다. 개인은 본인 문서만, 보호자는 ACTIVE 연결된 개인의 문서만 열람합니다. */
@Component
public class MedicalDocumentAccessPolicy {

    private static final List<CareRelationStatus> READABLE = List.of(CareRelationStatus.ACTIVE);

    private final CareRelationRepository relations;

    public MedicalDocumentAccessPolicy(CareRelationRepository relations) {
        this.relations = relations;
    }

    public void checkReadable(User actor, UUID patientId) {
        boolean allowed = switch (actor.getRole()) {
            case PATIENT -> actor.getId().equals(patientId);
            case CAREGIVER -> relations.findFirstByPatientIdAndCaregiverIdAndStatusIn(
                    patientId, actor.getId(), READABLE).isPresent();
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException(MedicalDocumentErrorCode.DOCUMENT_ACCESS_DENIED);
        }
    }
}
