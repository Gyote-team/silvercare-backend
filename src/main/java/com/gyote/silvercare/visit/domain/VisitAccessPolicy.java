package com.gyote.silvercare.visit.domain;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.visit.error.VisitErrorCode;

import org.springframework.stereotype.Component;

import java.util.UUID;

/** 본인 또는 ACTIVE로 연결된 보호자에게만 방문 관리 권한을 부여한다. */
@Component
public class VisitAccessPolicy {
    private final PatientRepository patients;
    private final CareRelationRepository relations;

    public VisitAccessPolicy(PatientRepository patients, CareRelationRepository relations) {
        this.patients = patients;
        this.relations = relations;
    }

    /** 개인의 대상 생략은 본인을 뜻하며 보호자는 대상 개인을 지정해야 한다. */
    public UUID requirePatient(User actor, UUID requested) {
        if (actor.getStatus() != UserStatus.ACTIVE)
            throw new BusinessException(VisitErrorCode.ACCESS_DENIED);
        if (actor.getRole() == UserRole.PATIENT) {
            UUID own =
                    patients.findByUserId(actor.getId())
                            .map(Patient::getId)
                            .orElseThrow(() -> new BusinessException(VisitErrorCode.ACCESS_DENIED));
            if (requested == null || own.equals(requested)) return own;
        } else if (actor.getRole() == UserRole.CAREGIVER
                && requested != null
                && relations.existsByPatientIdAndCaregiverIdAndStatus(
                        requested, actor.getId(), CareRelationStatus.ACTIVE)) {
            return requested;
        }
        throw new BusinessException(
                requested == null ? GlobalErrorCode.INVALID_REQUEST : VisitErrorCode.ACCESS_DENIED);
    }
}
