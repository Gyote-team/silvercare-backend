package com.gyote.silvercare.user.command.application;

import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.patient.domain.Patient;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.api.dto.response.WithdrawalResponse;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.error.UserErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/** 회원 탈퇴와 함께 활성 연결은 해제하고 대기 중인 연결 요청은 취소한다. */
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

        User user = users.findByKakaoIdForUpdate(kakaoId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(UserErrorCode.USER_ALREADY_WITHDRAWN);
        }

        Instant withdrawnAt = Instant.now();
        List<CareRelation> activeRelations = relationsFor(user, CareRelationStatus.ACTIVE);
        activeRelations.forEach(relation -> relation.revoke(withdrawnAt));
        relationsFor(user, CareRelationStatus.REQUESTED).forEach(relation -> relation.cancel(withdrawnAt));
        user.withdraw(withdrawnAt);

        return new WithdrawalResponse(UserStatus.WITHDRAWN, activeRelations.size());
    }

    /** 역할과 무관하게 돌보는 쪽과 돌봄받는 쪽 연결을 모두 모은다. */
    private List<CareRelation> relationsFor(User user, CareRelationStatus status) {
        List<CareRelation> asPatient = patients.findByUserId(user.getId())
                .map(Patient::getId)
                .map(patientId -> relations.findByPatientIdAndStatus(patientId, status))
                .orElseGet(List::of);
        List<CareRelation> asCaregiver = relations.findByCaregiverIdAndStatus(user.getId(), status);
        return Stream.concat(asPatient.stream(), asCaregiver.stream())
                .distinct()
                .toList();
    }
}
