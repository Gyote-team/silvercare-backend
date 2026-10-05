package com.gyote.silvercare.health_record.domain;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.health_record.error.HealthRecordErrorCode;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** 본인 프로필 또는 ACTIVE 보호자 관계인 개인의 건강 기록만 읽고 쓸 수 있게 한다. */
@Component
public class HealthRecordAccessPolicy {

    private final PatientRepository patients;
    private final CareRelationRepository relations;

    public HealthRecordAccessPolicy(PatientRepository patients, CareRelationRepository relations) {
        this.patients = patients;
        this.relations = relations;
    }

    public UUID requireAccessiblePatient(User actor, UUID requestedPatientId) {
        UUID patientId = requestedPatientId == null ? ownPatientId(actor) : requestedPatientId;
        if (patients.findByUserId(actor.getId()).map(Patient::getId).filter(patientId::equals).isPresent()) {
            return patientId;
        }
        if (patients.existsById(patientId)
                && relations.existsByPatientIdAndCaregiverIdAndStatus(
                patientId, actor.getId(), CareRelationStatus.ACTIVE)) {
            return patientId;
        }
        throw new BusinessException(HealthRecordErrorCode.RECORD_ACCESS_DENIED);
    }

    private UUID ownPatientId(User actor) {
        return patients.findByUserId(actor.getId())
                .map(Patient::getId)
                .orElseThrow(() -> new BusinessException(HealthRecordErrorCode.PATIENT_ID_REQUIRED));
    }
}
