package com.gyote.silvercare.medical_document.query.application;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.error.AiDocumentErrorCode;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CareRelationPermissionService {

    private final PatientRepository patients;
    private final CareRelationRepository relations;

    /** 요청 대상이 내 건강 프로필이거나 ACTIVE 돌봄 관계의 개인인지 검증한다. */
    public UUID resolveTargetPatient(User actor, UUID requestedPatientId) {
        if (requestedPatientId == null) {
            if (patients.findByUserId(actor.getId()).isPresent()) {
                return ownPatientId(actor);
            }
            throw new BusinessException(AiDocumentErrorCode.PATIENT_REQUIRED);
        }
        if (patients.findByUserId(actor.getId()).map(Patient::getId).filter(requestedPatientId::equals).isPresent()) {
            return requestedPatientId;
        }
        requireCaregiverAccess(actor, requestedPatientId);
        return requestedPatientId;
    }

    /** 문서 소유 환자에 대한 현재 사용자의 조회 권한을 검증한다. */
    public UUID requireDocumentAccess(User actor, UUID documentPatientId) {
        if (patients.findByUserId(actor.getId()).map(Patient::getId).filter(documentPatientId::equals).isPresent()) {
            return documentPatientId;
        }
        requireCaregiverAccess(actor, documentPatientId);
        return documentPatientId;
    }

    /** 보호자와 환자 사이에 활성화된 돌봄 관계가 있는지 검증한다. */
    private void requireCaregiverAccess(User caregiver, UUID patientId) {
        if (!patients.existsById(patientId)
                || !relations.existsByPatientIdAndCaregiverIdAndStatus(
                patientId,
                caregiver.getId(),
                CareRelationStatus.ACTIVE
        )) {
            throw new BusinessException(AiDocumentErrorCode.DOCUMENT_ACCESS_DENIED);
        }
    }

    /** 환자 사용자와 연결된 환자 식별자를 조회한다. */
    private UUID ownPatientId(User patientUser) {
        return patients.findByUserId(patientUser.getId())
                .map(Patient::getId)
                .orElseThrow(() -> new BusinessException(AiDocumentErrorCode.DOCUMENT_ACCESS_DENIED));
    }
}
