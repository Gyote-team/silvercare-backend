package com.gyote.silvercare.care_relation.query.application;

import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.care_relation.query.model.CareRelationView;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.query.application.UserQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Read-only care-relation queries and response assembly. */
@Service
@Transactional(readOnly = true)
public class CareRelationQueryService {

    private final CareRelationRepository relations;
    private final UserRepository users;
    private final UserQueryService userQueries;
    private final PatientRepository patients;

    public CareRelationQueryService(
            CareRelationRepository relations,
            UserRepository users,
            UserQueryService userQueries,
            PatientRepository patients
    ) {
        this.relations = relations;
        this.users = users;
        this.userQueries = userQueries;
        this.patients = patients;
    }

    public User requireUser(String kakaoId) {
        return userQueries.requireByKakaoId(kakaoId);
    }

    public List<CareRelationView> listFor(User me) {
        List<CareRelation> rows = me.getRole() == UserRole.PATIENT
                ? relations.findByPatientIdOrderByRequestedAtDesc(patientIdFor(me))
                : relations.findByCaregiverIdOrderByRequestedAtDesc(me.getId());
        Map<UUID, String> namesById = users.findAllById(counterpartIds(rows, me)).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
        return rows.stream().map(row -> toView(row, me, namesById)).toList();
    }

    private Set<UUID> counterpartIds(List<CareRelation> rows, User me) {
        boolean patientSide = me.getRole() == UserRole.PATIENT;
        return rows.stream()
                .map(row -> patientSide ? row.getCaregiverId() : patientUserId(row))
                .collect(Collectors.toSet());
    }

    private CareRelationView toView(CareRelation row, User me, Map<UUID, String> namesById) {
        boolean patientSide = me.getRole() == UserRole.PATIENT;
        var otherId = patientSide ? row.getCaregiverId() : patientUserId(row);
        String name = namesById.getOrDefault(otherId, "이용자");
        boolean requested = row.getStatus() == CareRelationStatus.REQUESTED;
        boolean active = row.getStatus() == CareRelationStatus.ACTIVE;
        return new CareRelationView(row.getId(), row.getPatientId(), name, statusLabel(row.getStatus()), row.getStatus(),
                patientSide && requested, patientSide && requested, !patientSide && requested, active,
                row.getAcceptedAt());
    }

    private static String statusLabel(CareRelationStatus status) {
        return switch (status) {
            case REQUESTED -> "대기";
            case ACTIVE -> "연결됨";
            case REJECTED -> "거절";
            case CANCELED -> "취소";
            case REVOKED -> "해제";
        };
    }

    private UUID patientIdFor(User user) {
        return patients.findByUserId(user.getId())
                .map(Patient::getId)
                .orElseThrow(() -> new IllegalStateException("환자 프로필이 없습니다."));
    }

    private UUID patientUserId(CareRelation relation) {
        return patients.findById(relation.getPatientId())
                .map(Patient::getUserId)
                .orElseThrow(() -> new IllegalStateException("관계의 환자 프로필이 없습니다."));
    }
}
