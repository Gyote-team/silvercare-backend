package com.gyote.silvercare.health_record.domain;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.*;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.health_record.error.HealthRecordErrorCode;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class HealthRecordAccessPolicy {
    private final PatientRepository patients;
    private final CareRelationRepository relations;
    public HealthRecordAccessPolicy(PatientRepository patients, CareRelationRepository relations) {
        this.patients=patients; this.relations=relations;
    }
    /** 현재 개인 계정 본인 또는 ACTIVE로 연결된 대상 개인만 허용한다. */
    public UUID requirePatient(User actor, UUID requested) {
        if (actor.getStatus()!=UserStatus.ACTIVE) throw new BusinessException(HealthRecordErrorCode.ACCESS_DENIED);
        if (actor.getRole()==UserRole.PATIENT) {
            UUID own = patients.findByUserId(actor.getId()).map(Patient::getId)
                    .orElseThrow(() -> new BusinessException(HealthRecordErrorCode.ACCESS_DENIED));
            if (requested==null || own.equals(requested)) return own;
        } else if (actor.getRole()==UserRole.CAREGIVER && requested!=null
                && relations.existsByPatientIdAndCaregiverIdAndStatus(requested, actor.getId(), CareRelationStatus.ACTIVE)) {
            return requested;
        }
        throw new BusinessException(requested==null ? HealthRecordErrorCode.INVALID_REQUEST : HealthRecordErrorCode.ACCESS_DENIED);
    }
}
