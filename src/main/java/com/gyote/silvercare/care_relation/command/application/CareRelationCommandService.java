package com.gyote.silvercare.care_relation.command.application;

import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationCode;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.care_relation.error.CareRelationErrorCode;
import com.gyote.silvercare.global.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** State-changing care-relation use cases only. */
@Service
public class CareRelationCommandService {

    private static final List<CareRelationStatus> ALIVE = List.of(
            CareRelationStatus.REQUESTED, CareRelationStatus.ACTIVE
    );

    private final CareRelationRepository relations;
    private final UserRepository users;

    public CareRelationCommandService(CareRelationRepository relations, UserRepository users) {
        this.relations = relations;
        this.users = users;
    }

    @Transactional
    public CareRelation request(User caregiver, String rawCode) {
        if (caregiver.getRole() != UserRole.CAREGIVER) {
            throw new BusinessException(CareRelationErrorCode.CAREGIVER_ONLY);
        }
        User patient = users.findByInviteCode(CareRelationCode.normalize(rawCode))
                .filter(found -> found.getRole() == UserRole.PATIENT)
                .orElseThrow(() -> new BusinessException(CareRelationErrorCode.INVITE_CODE_NOT_FOUND));
        if (patient.getId().equals(caregiver.getId())) {
            throw new BusinessException(CareRelationErrorCode.SELF_RELATION_NOT_ALLOWED);
        }
        if (relations.findFirstByPatientIdAndCaregiverIdAndStatusIn(
                patient.getId(), caregiver.getId(), ALIVE).isPresent()) {
            throw new BusinessException(CareRelationErrorCode.RELATION_ALREADY_EXISTS);
        }
        CareRelation created = new CareRelation();
        created.setPatientId(patient.getId());
        created.setCaregiverId(caregiver.getId());
        created.setStatus(CareRelationStatus.REQUESTED);
        return relations.save(created);
    }

    @Transactional
    public CareRelation accept(User patient, UUID relationId) {
        CareRelation relation = requireOwned(patient, relationId, true);
        if (relation.getStatus() != CareRelationStatus.REQUESTED) {
            throw new BusinessException(CareRelationErrorCode.INVALID_RELATION_STATE);
        }
        relation.setStatus(CareRelationStatus.ACTIVE);
        relation.setAcceptedAt(Instant.now());
        return relation;
    }

    @Transactional
    public CareRelation reject(User patient, UUID relationId) {
        CareRelation relation = requireOwned(patient, relationId, true);
        if (relation.getStatus() != CareRelationStatus.REQUESTED) {
            throw new BusinessException(CareRelationErrorCode.INVALID_RELATION_STATE);
        }
        relation.setStatus(CareRelationStatus.REJECTED);
        relation.setEndedAt(Instant.now());
        return relation;
    }

    @Transactional
    public CareRelation cancel(User caregiver, UUID relationId) {
        CareRelation relation = requireOwned(caregiver, relationId, false);
        if (relation.getStatus() != CareRelationStatus.REQUESTED) {
            throw new BusinessException(CareRelationErrorCode.INVALID_RELATION_STATE);
        }
        relation.setStatus(CareRelationStatus.CANCELED);
        relation.setEndedAt(Instant.now());
        return relation;
    }

    @Transactional
    public CareRelation revoke(User actor, UUID relationId) {
        CareRelation relation = relations.findById(relationId)
                .orElseThrow(() -> new BusinessException(CareRelationErrorCode.RELATION_NOT_FOUND));
        boolean mine = actor.getId().equals(relation.getPatientId()) || actor.getId().equals(relation.getCaregiverId());
        if (!mine) {
            throw new BusinessException(CareRelationErrorCode.RELATION_ACCESS_DENIED);
        }
        if (relation.getStatus() != CareRelationStatus.ACTIVE) {
            throw new BusinessException(CareRelationErrorCode.INVALID_RELATION_STATE);
        }
        relation.setStatus(CareRelationStatus.REVOKED);
        relation.setEndedAt(Instant.now());
        return relation;
    }

    private CareRelation requireOwned(User actor, UUID relationId, boolean asPatient) {
        CareRelation relation = relations.findById(relationId)
                .orElseThrow(() -> new BusinessException(CareRelationErrorCode.RELATION_NOT_FOUND));
        UUID expected = asPatient ? relation.getPatientId() : relation.getCaregiverId();
        if (!expected.equals(actor.getId())) {
            throw new BusinessException(CareRelationErrorCode.RELATION_ACCESS_DENIED);
        }
        if (asPatient && actor.getRole() != UserRole.PATIENT) {
            throw new BusinessException(CareRelationErrorCode.RELATION_ACCESS_DENIED);
        }
        if (!asPatient && actor.getRole() != UserRole.CAREGIVER) {
            throw new BusinessException(CareRelationErrorCode.RELATION_ACCESS_DENIED);
        }
        return relation;
    }
}
