package com.gyote.silvercare.user.command.application;

import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.api.dto.response.WithdrawalResponse;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.error.UserErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** 회원 탈퇴와 활성 보호자 연결 해제를 함께 처리한다. */
@Service
public class UserWithdrawalService {

    private final UserRepository users;
    private final PatientRepository patients;
    private final CareRelationRepository relations;

    public UserWithdrawalService(
            UserRepository users,
            PatientRepository patients,
            CareRelationRepository relations
    ) {
        this.users = users;
        this.patients = patients;
        this.relations = relations;
    }

    @Transactional
    public WithdrawalResponse withdraw(String kakaoId, Boolean confirmed) {
        if (!Boolean.TRUE.equals(confirmed)) {
            throw new BusinessException(UserErrorCode.WITHDRAWAL_CONFIRMATION_REQUIRED);
        }

        User user = users.findByKakaoId(kakaoId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(UserErrorCode.USER_ALREADY_WITHDRAWN);
        }

        Instant withdrawnAt = Instant.now();
        List<CareRelation> activeRelations = activeRelationsFor(user);
        activeRelations.forEach(relation -> relation.revoke(withdrawnAt));
        user.withdraw(withdrawnAt);

        return new WithdrawalResponse(UserStatus.WITHDRAWN, activeRelations.size());
    }

    private List<CareRelation> activeRelationsFor(User user) {
        if (user.getRole() == UserRole.CAREGIVER) {
            return relations.findByCaregiverIdAndStatus(user.getId(), CareRelationStatus.ACTIVE);
        }
        if (user.getRole() == UserRole.PATIENT) {
            return patients.findByUserId(user.getId())
                    .map(Patient::getId)
                    .map(patientId -> relations.findByPatientIdAndStatus(patientId, CareRelationStatus.ACTIVE))
                    .orElseGet(List::of);
        }
        return List.of();
    }
}
