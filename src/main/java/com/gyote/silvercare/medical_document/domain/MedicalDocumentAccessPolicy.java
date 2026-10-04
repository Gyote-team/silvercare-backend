package com.gyote.silvercare.medical_document.domain;

import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 의료 문서 열람 권한 규칙입니다. 내 건강 프로필 또는 ACTIVE 돌봄 관계의 문서만 열람합니다. */
@Component
public class MedicalDocumentAccessPolicy {

    private static final List<CareRelationStatus> READABLE = List.of(CareRelationStatus.ACTIVE);

    private final CareRelationRepository relations;
    private final PatientRepository patients;

    public MedicalDocumentAccessPolicy(CareRelationRepository relations, PatientRepository patients) {
        this.relations = relations;
        this.patients = patients;
    }

    /** 열람 가능 여부를 반환합니다. 한 계정은 본인 프로필과 돌보는 개인을 함께 조회할 수 있습니다. */
    public boolean canRead(User actor, UUID patientId) {
        return ownPatientId(actor).filter(patientId::equals).isPresent()
                || relations.findFirstByPatientIdAndCaregiverIdAndStatusIn(
                patientId, actor.getId(), READABLE).isPresent();
    }

    /** 열람 권한이 없으면 DOCUMENT_ACCESS_DENIED(403)를 던집니다. 목록 조회에서 사용합니다. */
    public void checkReadable(User actor, UUID patientId) {
        if (!canRead(actor, patientId)) {
            throw new BusinessException(MedicalDocumentErrorCode.DOCUMENT_ACCESS_DENIED);
        }
    }

    /** 삭제 가능 여부를 반환합니다. 내 프로필 문서는 모두, 돌보는 개인 문서는 내가 올린 것만 삭제할 수 있습니다. */
    public boolean canDelete(User me, MedicalDocument document) {
        return ownPatientId(me).filter(document.getPatientId()::equals).isPresent()
                || (me.getId().equals(document.getUploaderUserId()) && canRead(me, document.getPatientId()));
    }

    private Optional<UUID> ownPatientId(User actor) {
        return patients.findByUserId(actor.getId()).map(Patient::getId);
    }
}
