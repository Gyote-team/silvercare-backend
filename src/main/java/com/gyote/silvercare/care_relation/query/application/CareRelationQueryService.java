package com.gyote.silvercare.care_relation.query.application;

import com.gyote.silvercare.care_relation.api.response.CareRelationResponse;
import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.query.application.UserQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read-only care-relation queries and response assembly. */
@Service
@Transactional(readOnly = true)
public class CareRelationQueryService {

    private final CareRelationRepository relations;
    private final UserRepository users;
    private final UserQueryService userQueries;

    public CareRelationQueryService(CareRelationRepository relations, UserRepository users, UserQueryService userQueries) {
        this.relations = relations;
        this.users = users;
        this.userQueries = userQueries;
    }

    public User requireUser(String kakaoId) {
        return userQueries.requireByKakaoId(kakaoId);
    }

    public List<CareRelationResponse> listFor(User me) {
        List<CareRelation> rows = me.getRole() == UserRole.PATIENT
                ? relations.findByPatientIdOrderByRequestedAtDesc(me.getId())
                : relations.findByCaregiverIdOrderByRequestedAtDesc(me.getId());
        return rows.stream().map(row -> toResponse(row, me)).toList();
    }

    private CareRelationResponse toResponse(CareRelation row, User me) {
        boolean patientSide = me.getRole() == UserRole.PATIENT;
        var otherId = patientSide ? row.getCaregiverId() : row.getPatientId();
        String name = users.findById(otherId).map(User::getName).orElse("이용자");
        boolean requested = row.getStatus() == CareRelationStatus.REQUESTED;
        boolean active = row.getStatus() == CareRelationStatus.ACTIVE;
        return new CareRelationResponse(row.getId(), name, statusLabel(row.getStatus()), row.getStatus(),
                patientSide && requested, patientSide && requested, !patientSide && requested, active);
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
}
