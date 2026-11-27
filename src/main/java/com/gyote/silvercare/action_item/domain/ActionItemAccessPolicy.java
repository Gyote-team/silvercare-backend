package com.gyote.silvercare.action_item.domain;

import com.gyote.silvercare.action_item.error.ActionItemErrorCode;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.medical_document.domain.MedicalDocumentAccessPolicy;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;

import org.springframework.stereotype.Component;

import java.util.UUID;

/** 문서 기반 후보도 본인 또는 현재 ACTIVE 보호자만 조회하도록 검증한다. */
@Component
public class ActionItemAccessPolicy {
    private final PatientRepository patients;
    private final MedicalDocumentAccessPolicy documents;

    public ActionItemAccessPolicy(
            PatientRepository patients, MedicalDocumentAccessPolicy documents) {
        this.patients = patients;
        this.documents = documents;
    }

    /** 개인의 대상 생략은 본인으로 해석하고 현재 문서 열람 범위를 검증한다. */
    public UUID requirePatient(User actor, UUID requestedPatientId) {
        if (actor.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ActionItemErrorCode.ACCESS_DENIED);
        }
        UUID target = requestedPatientId;
        if (target == null) {
            if (actor.getRole() != UserRole.PATIENT) {
                throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
            }
            target =
                    patients.findByUserId(actor.getId())
                            .map(Patient::getId)
                            .orElseThrow(
                                    () -> new BusinessException(ActionItemErrorCode.ACCESS_DENIED));
        }
        if (!documents.canRead(actor, target)) {
            throw new BusinessException(ActionItemErrorCode.ACCESS_DENIED);
        }
        return target;
    }
}
