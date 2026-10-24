package com.gyote.silvercare.care_relation.application;

import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.domain.CareRelationCode;
import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.care_relation.error.CareRelationErrorCode;
import com.gyote.silvercare.care_relation.query.application.CareRelationQueryService;
import com.gyote.silvercare.care_relation.query.model.CareRelationView;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.query.application.UserQueryService;
import com.gyote.silvercare.user.command.application.UserWithdrawalService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.user.error.UserErrorCode;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

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
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
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
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
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
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
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
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
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
    void caregiverWithdrawnAfterAuthenticationCannotRequest() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        caregiver.setStatus(UserStatus.WITHDRAWN);

        assertThatThrownBy(() -> cares.request(caregiver, accounts.patientInviteCode(patient)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.USER_ALREADY_WITHDRAWN);
        assertThat(relations.findByCaregiverIdOrderByRequestedAtDesc(caregiver.getId())).isEmpty();
    }

    @Test
    void acceptIsRejectedWhenCaregiverWithdrewBeforeLockWasAcquired() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        CareRelation requested = cares.request(caregiver, accounts.patientInviteCode(patient));
        caregiver.setStatus(UserStatus.WITHDRAWN);

        assertThatThrownBy(() -> cares.accept(patient, requested.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(CareRelationErrorCode.INVALID_RELATION_STATE);
        assertThat(requested.getStatus()).isEqualTo(CareRelationStatus.REQUESTED);
    }

    @Test
    void eitherSideCanRevokeActiveLink() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
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
    void bothParticipantsSeeRelationDetailWithTimestamps() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        CareRelationQueryService queries = new CareRelationQueryService(relations, users, new UserQueryService(users), patients);
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
        cares.revoke(caregiver, requested.getId());

        CareRelationView patientView = queries.detailFor(patient, requested.getId());
        CareRelationView caregiverView = queries.detailFor(caregiver, requested.getId());

        assertThat(patientView.counterpartName()).isEqualTo("김민지");
        assertThat(patientView.counterpartRole()).isEqualTo(UserRole.CAREGIVER);
        assertThat(patientView.caregiverId()).isEqualTo(caregiver.getId());
        assertThat(caregiverView.counterpartName()).isEqualTo("김순자");
        assertThat(caregiverView.counterpartRole()).isEqualTo(UserRole.PATIENT);
        assertThat(patientView.status()).isEqualTo(CareRelationStatus.REVOKED);
        assertThat(patientView.requestedAt()).isNotNull();
        assertThat(patientView.acceptedAt()).isNotNull();
        assertThat(patientView.endedAt()).isNotNull();
    }

    @Test
    void strangerCannotSeeRelationDetailAndUnknownIdIsNotFound() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        CareRelationQueryService queries = new CareRelationQueryService(relations, users, new UserQueryService(users), patients);
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        User stranger = accounts.chooseRole(
                accounts.loginOrRegister("kakao-other", "이낯선").getKakaoId(),
                UserRole.CAREGIVER
        );
        CareRelation requested = cares.request(caregiver, accounts.patientInviteCode(patient));

        assertThatThrownBy(() -> queries.detailFor(stranger, requested.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(CareRelationErrorCode.RELATION_ACCESS_DENIED);
        assertThatThrownBy(() -> queries.detailFor(patient, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(CareRelationErrorCode.RELATION_NOT_FOUND);
    }
}
