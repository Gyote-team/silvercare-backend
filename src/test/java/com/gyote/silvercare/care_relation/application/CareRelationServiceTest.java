package com.gyote.silvercare.care_relation.application;

import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.domain.CareRelationCode;
import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
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

    @Test
    void caregiverRequestsWithPatientInviteCode() {
        UserAccountService accounts = new UserAccountService(users);
        CareRelationCommandService cares = new CareRelationCommandService(relations, users);
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );

        CareRelation created = cares.request(caregiver, CareRelationCode.display(patient.getInviteCode()));

        assertThat(created.getStatus()).isEqualTo(CareRelationStatus.REQUESTED);
        assertThat(created.getPatientId()).isEqualTo(patient.getId());
        assertThatThrownBy(() -> cares.request(caregiver, patient.getInviteCode()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void patientAcceptsThenCaregiverCannotAccept() {
        UserAccountService accounts = new UserAccountService(users);
        CareRelationCommandService cares = new CareRelationCommandService(relations, users);
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        CareRelation requested = cares.request(caregiver, patient.getInviteCode());

        CareRelation accepted = cares.accept(patient, requested.getId());

        assertThat(accepted.getStatus()).isEqualTo(CareRelationStatus.ACTIVE);
        assertThatThrownBy(() -> cares.accept(caregiver, requested.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unknownOrSelfCodeIsRejected() {
        UserAccountService accounts = new UserAccountService(users);
        CareRelationCommandService cares = new CareRelationCommandService(relations, users);
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );

        assertThatThrownBy(() -> cares.request(caregiver, "AAAAAA"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> cares.request(caregiver, caregiver.getInviteCode()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void eitherSideCanRevokeActiveLink() {
        UserAccountService accounts = new UserAccountService(users);
        CareRelationCommandService cares = new CareRelationCommandService(relations, users);
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        CareRelation requested = cares.request(caregiver, patient.getInviteCode());
        cares.accept(patient, requested.getId());

        CareRelation revoked = cares.revoke(patient, requested.getId());

        assertThat(revoked.getStatus()).isEqualTo(CareRelationStatus.REVOKED);
        assertThatThrownBy(() -> cares.revoke(caregiver, requested.getId()))
                .isInstanceOf(BusinessException.class);
    }
}
