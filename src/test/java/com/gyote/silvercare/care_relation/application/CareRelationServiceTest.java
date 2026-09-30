package com.gyote.silvercare.care_relation.application;

import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.query.application.CareRelationQueryService;
import com.gyote.silvercare.care_relation.query.model.CareRelationView;
import com.gyote.silvercare.care_relation.domain.CareRelationCode;
import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.care_relation.error.CareRelationErrorCode;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.command.application.UserWithdrawalService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.query.application.UserQueryService;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CareRelationServiceTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private CareRelationRepository relations;

    @Autowired
    private PatientRepository patients;

    @Test
    void caregiverRequestsWithPatientInviteCode() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users);
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );

        String inviteCode = accounts.patientInviteCode(patient);
        CareRelation created = cares.request(caregiver, CareRelationCode.display(inviteCode));

        assertThat(created.getStatus()).isEqualTo(CareRelationStatus.REQUESTED);
        assertThat(created.getPatientId()).isEqualTo(patients.findByUserId(patient.getId()).orElseThrow().getId());
        assertThatThrownBy(() -> cares.request(caregiver, inviteCode))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void patientAcceptsThenCaregiverCannotAccept() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users);
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        CareRelation requested = cares.request(caregiver, accounts.patientInviteCode(patient));

        CareRelation accepted = cares.accept(patient, requested.getId());

        assertThat(accepted.getStatus()).isEqualTo(CareRelationStatus.ACTIVE);
        assertThatThrownBy(() -> cares.accept(caregiver, requested.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unknownOrSelfCodeIsRejected() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users);
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );

        assertThatThrownBy(() -> cares.request(caregiver, "AAAAAA"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void withdrawnPatientInviteCodeIsRejected() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users);
        UserWithdrawalService withdrawals = new UserWithdrawalService(users, patients, relations);
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        String inviteCode = accounts.patientInviteCode(patient);

        withdrawals.withdraw(patient.getKakaoId(), true);

        assertThatThrownBy(() -> cares.request(caregiver, inviteCode))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(CareRelationErrorCode.INVITE_CODE_NOT_FOUND);
        assertThat(relations.findByCaregiverIdOrderByRequestedAtDesc(caregiver.getId())).isEmpty();
    }

    @Test
    void eitherSideCanRevokeActiveLink() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users);
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        CareRelation requested = cares.request(caregiver, accounts.patientInviteCode(patient));
        cares.accept(patient, requested.getId());

        CareRelation revoked = cares.revoke(patient, requested.getId());

        assertThat(revoked.getStatus()).isEqualTo(CareRelationStatus.REVOKED);
        assertThatThrownBy(() -> cares.revoke(caregiver, requested.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void relationResponseViewKeepsLegacyActionsAndProvidesContractMetadata() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users);
        CareRelationQueryService queries = new CareRelationQueryService(
                relations, users, new UserQueryService(users), patients
        );
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        CareRelation relation = cares.request(caregiver, accounts.patientInviteCode(patient));

        CareRelationView view = queries.listFor(patient).get(0);

        assertThat(view.id()).isEqualTo(relation.getId());
        assertThat(view.relationId()).isEqualTo(relation.getId());
        assertThat(view.counterpartRole()).isEqualTo(UserRole.CAREGIVER);
        assertThat(view.requestedAt()).isNotNull();
        assertThat(view.acceptedAt()).isNull();
        assertThat(view.revokedAt()).isNull();
        assertThat(view.canAccept()).isTrue();
        assertThat(view.canReject()).isTrue();
    }
}
