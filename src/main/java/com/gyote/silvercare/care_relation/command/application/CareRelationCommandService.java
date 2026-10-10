package com.gyote.silvercare.care_relation.command.application;

import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationCode;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.error.UserErrorCode;
import com.gyote.silvercare.care_relation.error.CareRelationErrorCode;
import com.gyote.silvercare.global.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** State-changing care-relation use cases only. */
@Service
public class CareRelationCommandService {

    private static final List<CareRelationStatus> ALIVE = List.of(
            CareRelationStatus.REQUESTED, CareRelationStatus.ACTIVE
    );

    private final CareRelationRepository relations;
    private final PatientRepository patients;
    private final UserRepository users;
    private org.springframework.context.ApplicationEventPublisher events = event -> {};

    /** 시스템 활동을 같은 트랜잭션의 알림 처리기로 전달한다. */
    @org.springframework.beans.factory.annotation.Autowired
    public void setEventPublisher(org.springframework.context.ApplicationEventPublisher events) { this.events = events; }

    public CareRelationCommandService(
            CareRelationRepository relations,
            PatientRepository patients,
            UserRepository users
    ) {
        this.relations = relations;
        this.patients = patients;
        this.users = users;
    }

    /** 보호자의 초대 코드 입력으로 REQUESTED 연결 관계를 생성한다. */
    @Transactional
    public CareRelation request(User caregiver, String rawCode) {
        if (caregiver.getRole() != UserRole.CAREGIVER) {
            throw new BusinessException(CareRelationErrorCode.CAREGIVER_ONLY);
        }
        Patient patient = patients.findByInviteCode(CareRelationCode.normalize(rawCode))
                .orElseThrow(() -> new BusinessException(CareRelationErrorCode.INVITE_CODE_NOT_FOUND));
        if (patient.getUserId().equals(caregiver.getId())) {
            throw new BusinessException(CareRelationErrorCode.SELF_RELATION_NOT_ALLOWED);
        }
        Map<UUID, User> locked = lockUsers(patient.getUserId(), caregiver.getId());
        if (!isActive(locked.get(patient.getUserId()))) {
            throw new BusinessException(CareRelationErrorCode.INVITE_CODE_NOT_FOUND);
        }
        if (!isActive(locked.get(caregiver.getId()))) {
            throw new BusinessException(UserErrorCode.USER_ALREADY_WITHDRAWN);
        }
        if (sameAccountGroup(locked.get(patient.getUserId()), locked.get(caregiver.getId()))) {
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
        CareRelation saved = relations.save(created);
        notifyActivity("RELATION_REQUESTED", caregiver, saved);
        return saved;
    }

    /** 관계의 개인이 REQUESTED 연결을 ACTIVE로 수락한다. */
    @Transactional
    public CareRelation accept(User patient, UUID relationId) {
        CareRelationRepository.Participants participants = relations.findParticipantsById(relationId)
                .orElseThrow(() -> new BusinessException(CareRelationErrorCode.RELATION_NOT_FOUND));
        Map<UUID, User> locked = lockUsers(patientUserId(participants.getPatientId()), participants.getCaregiverId());
        if (!locked.values().stream().allMatch(this::isActive) || locked.size() != 2) {
            throw new BusinessException(CareRelationErrorCode.INVALID_RELATION_STATE);
        }
        CareRelation relation = requireOwned(patient, relationId, true);
        if (relation.getStatus() != CareRelationStatus.REQUESTED) {
            throw new BusinessException(CareRelationErrorCode.INVALID_RELATION_STATE);
        }
        relation.setStatus(CareRelationStatus.ACTIVE);
        relation.setAcceptedAt(Instant.now());
        notifyActivity("RELATION_ACCEPTED", patient, relation);
        return relation;
    }

    /** 관계의 개인이 REQUESTED 연결을 REJECTED로 거절한다. */
    @Transactional
    public CareRelation reject(User patient, UUID relationId) {
        CareRelation relation = requireOwned(patient, relationId, true);
        if (relation.getStatus() != CareRelationStatus.REQUESTED) {
            throw new BusinessException(CareRelationErrorCode.INVALID_RELATION_STATE);
        }
        relation.setStatus(CareRelationStatus.REJECTED);
        relation.setEndedAt(Instant.now());
        notifyActivity("RELATION_REJECTED", patient, relation);
        return relation;
    }

    /** 요청을 만든 보호자가 REQUESTED 연결을 CANCELED로 취소한다. */
    @Transactional
    public CareRelation cancel(User caregiver, UUID relationId) {
        CareRelation relation = requireOwned(caregiver, relationId, false);
        if (relation.getStatus() != CareRelationStatus.REQUESTED) {
            throw new BusinessException(CareRelationErrorCode.INVALID_RELATION_STATE);
        }
        relation.setStatus(CareRelationStatus.CANCELED);
        relation.setEndedAt(Instant.now());
        notifyActivity("RELATION_CANCELED", caregiver, relation);
        return relation;
    }

    /** 연결 당사자가 ACTIVE 연결을 REVOKED로 해제한다. */
    @Transactional
    public CareRelation revoke(User actor, UUID relationId) {
        CareRelation relation = relations.findById(relationId)
                .orElseThrow(() -> new BusinessException(CareRelationErrorCode.RELATION_NOT_FOUND));
        boolean mine = patientUserId(relation).equals(actor.getId()) || actor.getId().equals(relation.getCaregiverId());
        if (!mine) {
            throw new BusinessException(CareRelationErrorCode.RELATION_ACCESS_DENIED);
        }
        if (relation.getStatus() != CareRelationStatus.ACTIVE) {
            throw new BusinessException(CareRelationErrorCode.INVALID_RELATION_STATE);
        }
        relation.setStatus(CareRelationStatus.REVOKED);
        relation.setEndedAt(Instant.now());
        notifyActivity("RELATION_REVOKED", actor, relation);
        return relation;
    }

    private CareRelation requireOwned(User actor, UUID relationId, boolean asPatient) {
        CareRelation relation = relations.findById(relationId)
                .orElseThrow(() -> new BusinessException(CareRelationErrorCode.RELATION_NOT_FOUND));
        UUID expected = asPatient ? patientUserId(relation) : relation.getCaregiverId();
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

    private void notifyActivity(String type, User actor, CareRelation relation) {
        events.publishEvent(new com.gyote.silvercare.notification.domain.SystemActivityEvent(
                type, relation.getPatientId(), relation.getCaregiverId(), actor.getId(), relation.getId()));
    }

    /**
     * 관계를 만들거나 활성화하기 전에 당사자 사용자 행을 잠근다.
     * 탈퇴도 같은 사용자 행을 잠그므로, 잠금 이후에 읽은 상태와 관계는 탈퇴와 겹치지 않는다.
     */
    private Map<UUID, User> lockUsers(UUID first, UUID second) {
        return users.findAllByIdForUpdate(List.of(first, second)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private boolean isActive(User user) {
        return user != null && user.getStatus() == UserStatus.ACTIVE;
    }

    /** 같은 사람의 개인 계정과 보호자 계정은 서로 연결할 수 없다. */
    private boolean sameAccountGroup(User patientUser, User caregiver) {
        return patientUser.getAccountGroupId().equals(caregiver.getAccountGroupId());
    }

    private UUID patientUserId(CareRelation relation) {
        return patientUserId(relation.getPatientId());
    }

    private UUID patientUserId(UUID patientId) {
        return patients.findById(patientId)
                .map(Patient::getUserId)
                .orElseThrow(() -> new BusinessException(CareRelationErrorCode.RELATION_NOT_FOUND));
    }
}
