package com.gyote.silvercare.medical_document.query.application;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.error.AiDocumentErrorCode;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CareRelationPermissionService {

    private final PatientRepository patients;
    private final CareRelationRepository relations;
    public CareRelationPermissionService(
            PatientRepository patients,
            CareRelationRepository relations
    ) {
        this.patients = patients;
        this.relations = relations;
    }

    /** 요청 조건과 사용자 역할을 기준으로 조회 대상 환자를 결정한다. */
    public UUID resolveTargetPatient(User actor, UUID requestedPatientId) {
        if (actor.getRole() == UserRole.PATIENT) {
            UUID ownPatientId = ownPatientId(actor);
            if (requestedPatientId != null && !ownPatientId.equals(requestedPatientId)) {
                throw new BusinessException(AiDocumentErrorCode.DOCUMENT_ACCESS_DENIED);
            }
            return ownPatientId;
        }

        if (actor.getRole() != UserRole.CAREGIVER) {
            throw new BusinessException(AiDocumentErrorCode.DOCUMENT_ACCESS_DENIED);
        }
        if (requestedPatientId == null) {
            throw new BusinessException(AiDocumentErrorCode.PATIENT_REQUIRED);
        }
        requireCaregiverAccess(actor, requestedPatientId);
        return requestedPatientId;
    }

    /** 문서 소유 환자에 대한 현재 사용자의 조회 권한을 검증한다. */
    public UUID requireDocumentAccess(User actor, UUID documentPatientId) {
        if (actor.getRole() == UserRole.PATIENT) {
            if (!ownPatientId(actor).equals(documentPatientId)) {
                throw new BusinessException(AiDocumentErrorCode.DOCUMENT_ACCESS_DENIED);
            }
            return documentPatientId;
        }

        if (actor.getRole() == UserRole.CAREGIVER) {
            requireCaregiverAccess(actor, documentPatientId);
            return documentPatientId;
        }

        throw new BusinessException(AiDocumentErrorCode.DOCUMENT_ACCESS_DENIED);
    }

    /** 보호자와 환자 사이에 활성화된 돌봄 관계가 있는지 검증한다. */
    private void requireCaregiverAccess(User caregiver, UUID patientId) {
        var relation = relations.findFirstByPatientIdAndCaregiverIdAndStatus(
                patientId, caregiver.getId(), CareRelationStatus.ACTIVE
        );
        if (!patients.existsById(patientId)
                || relation.isEmpty()) {
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
