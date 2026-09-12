package com.gyote.silvercare.care_relation.command.application;

import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationCode;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
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
            throw new IllegalStateException("보호자만 연결을 요청할 수 있습니다");
        }
        User patient = users.findByInviteCode(CareRelationCode.normalize(rawCode))
                .filter(found -> found.getRole() == UserRole.PATIENT)
                .orElseThrow(() -> new IllegalArgumentException("없는 코드입니다"));
        if (patient.getId().equals(caregiver.getId())) {
            throw new IllegalArgumentException("자기 자신과는 연결할 수 없습니다");
        }
        if (relations.findFirstByPatientIdAndCaregiverIdAndStatusIn(
                patient.getId(), caregiver.getId(), ALIVE).isPresent()) {
            throw new IllegalStateException("이미 요청했거나 연결되어 있습니다");
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
            throw new IllegalStateException("대기 중인 요청만 수락할 수 있습니다");
        }
        relation.setStatus(CareRelationStatus.ACTIVE);
        relation.setAcceptedAt(Instant.now());
        return relation;
    }

    @Transactional
    public CareRelation reject(User patient, UUID relationId) {
        CareRelation relation = requireOwned(patient, relationId, true);
        if (relation.getStatus() != CareRelationStatus.REQUESTED) {
            throw new IllegalStateException("대기 중인 요청만 거절할 수 있습니다");
        }
        relation.setStatus(CareRelationStatus.REJECTED);
        relation.setEndedAt(Instant.now());
        return relation;
    }

    @Transactional
    public CareRelation cancel(User caregiver, UUID relationId) {
        CareRelation relation = requireOwned(caregiver, relationId, false);
        if (relation.getStatus() != CareRelationStatus.REQUESTED) {
            throw new IllegalStateException("대기 중인 요청만 취소할 수 있습니다");
        }
        relation.setStatus(CareRelationStatus.CANCELED);
        relation.setEndedAt(Instant.now());
        return relation;
    }

    @Transactional
    public CareRelation revoke(User actor, UUID relationId) {
        CareRelation relation = relations.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("연결을 찾을 수 없습니다"));
        boolean mine = actor.getId().equals(relation.getPatientId()) || actor.getId().equals(relation.getCaregiverId());
        if (!mine) {
            throw new IllegalStateException("이 연결을 처리할 수 없습니다");
        }
        if (relation.getStatus() != CareRelationStatus.ACTIVE) {
            throw new IllegalStateException("연결된 관계만 끊을 수 있습니다");
        }
        relation.setStatus(CareRelationStatus.REVOKED);
        relation.setEndedAt(Instant.now());
        return relation;
    }

    private CareRelation requireOwned(User actor, UUID relationId, boolean asPatient) {
        CareRelation relation = relations.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("연결을 찾을 수 없습니다"));
        UUID expected = asPatient ? relation.getPatientId() : relation.getCaregiverId();
        if (!expected.equals(actor.getId())) {
            throw new IllegalStateException("이 연결을 처리할 수 없습니다");
        }
        if (asPatient && actor.getRole() != UserRole.PATIENT) {
            throw new IllegalStateException("본인만 수락하거나 거절할 수 있습니다");
        }
        if (!asPatient && actor.getRole() != UserRole.CAREGIVER) {
            throw new IllegalStateException("보호자만 요청을 취소할 수 있습니다");
        }
        return relation;
    }
}
