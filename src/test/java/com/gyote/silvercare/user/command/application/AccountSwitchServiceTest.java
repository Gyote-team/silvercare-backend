package com.gyote.silvercare.user.command.application;

import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.domain.CareRelation;
import com.gyote.silvercare.care_relation.domain.CareRelationStatus;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.care_relation.error.CareRelationErrorCode;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.error.UserErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AccountSwitchServiceTest {

    @Test
    void fourNamedDemoAccountsAreDistinctAndCanSwitchBothWays() {
        UserAccountService accounts = new UserAccountService(users, patients);
        AccountSwitchService switches = new AccountSwitchService(users, accounts);
        java.util.Set<java.util.UUID> groups = new java.util.HashSet<>();
        for (String name : java.util.List.of("MINJI", "SOONJA", "JIHUN", "SEOYEON")) {
            User demo = accounts.ensureNamedDemoUser(name);
            assertThat(accounts.ensureNamedDemoUser(name).getId()).isEqualTo(demo.getId());
            groups.add(demo.getAccountGroupId());
            User personal = switches.switchTo(demo.getKakaoId(), UserRole.PATIENT);
            assertThat(accounts.patientInviteCode(personal)).hasSize(6);
            User caregiver = switches.switchTo(personal.getKakaoId(), UserRole.CAREGIVER);
            assertThat(caregiver.getAccountGroupId()).isEqualTo(demo.getAccountGroupId());
            assertThat(switches.switchTo(caregiver.getKakaoId(), UserRole.PATIENT).getId()).isEqualTo(personal.getId());
        }
        assertThat(groups).hasSize(4);
        assertThatThrownBy(() -> accounts.ensureNamedDemoUser("UNKNOWN")).isInstanceOf(BusinessException.class);
    }

    @Autowired
    private UserRepository users;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private CareRelationRepository relations;

    @Test
    void switchingToCaregiverCreatesLinkedAccountOnce() {
        UserAccountService accounts = new UserAccountService(users, patients);
        AccountSwitchService switches = new AccountSwitchService(users, accounts);
        User personal = accounts.chooseRole(accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(), UserRole.PATIENT);

        User caregiver = switches.switchTo("kakao-soonja", UserRole.CAREGIVER);
        User again = switches.switchTo("kakao-soonja", UserRole.CAREGIVER);

        assertThat(caregiver.getId()).isNotEqualTo(personal.getId());
        assertThat(caregiver.getRole()).isEqualTo(UserRole.CAREGIVER);
        assertThat(caregiver.getName()).isEqualTo("김순자");
        assertThat(caregiver.getKakaoId()).isEqualTo("kakao-soonja#CAREGIVER");
        assertThat(caregiver.getAccountGroupId()).isEqualTo(personal.getAccountGroupId());
        assertThat(caregiver.isGroupOwner()).isFalse();
        assertThat(personal.isGroupOwner()).isTrue();
        assertThat(again.getId()).isEqualTo(caregiver.getId());
        assertThat(users.count()).isEqualTo(2);
        assertThat(switches.linkedAccounts(personal)).extracting(User::getRole)
                .containsExactlyInAnyOrder(UserRole.PATIENT, UserRole.CAREGIVER);
    }

    @Test
    void switchingBackFromLinkedAccountReturnsOriginalAccount() {
        UserAccountService accounts = new UserAccountService(users, patients);
        AccountSwitchService switches = new AccountSwitchService(users, accounts);
        User personal = accounts.chooseRole(accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(), UserRole.PATIENT);
        User caregiver = switches.switchTo("kakao-soonja", UserRole.CAREGIVER);

        User back = switches.switchTo(caregiver.getKakaoId(), UserRole.PATIENT);

        assertThat(back.getId()).isEqualTo(personal.getId());
        assertThat(users.count()).isEqualTo(2);
    }

    @Test
    void switchingToPersonalCreatesInviteCode() {
        UserAccountService accounts = new UserAccountService(users, patients);
        AccountSwitchService switches = new AccountSwitchService(users, accounts);
        accounts.chooseRole(accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(), UserRole.CAREGIVER);

        User personal = switches.switchTo("kakao-minji", UserRole.PATIENT);

        assertThat(personal.getRole()).isEqualTo(UserRole.PATIENT);
        assertThat(personal.getKakaoId()).isEqualTo("kakao-minji#PATIENT");
        assertThat(accounts.patientInviteCode(personal)).hasSize(6);
    }

    @Test
    void accountWithoutRoleCannotSwitch() {
        UserAccountService accounts = new UserAccountService(users, patients);
        AccountSwitchService switches = new AccountSwitchService(users, accounts);
        accounts.loginOrRegister("kakao-minji", "김민지");

        assertThatThrownBy(() -> switches.switchTo("kakao-minji", UserRole.CAREGIVER))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.ROLE_NOT_SELECTED);
        assertThatThrownBy(() -> switches.switchTo("kakao-minji", UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_ROLE);
    }

    @Test
    void ownCaregiverAccountCannotConnectToOwnPersonalAccount() {
        UserAccountService accounts = new UserAccountService(users, patients);
        AccountSwitchService switches = new AccountSwitchService(users, accounts);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users);
        User personal = accounts.chooseRole(accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(), UserRole.PATIENT);
        User caregiver = switches.switchTo("kakao-soonja", UserRole.CAREGIVER);

        assertThatThrownBy(() -> cares.request(caregiver, accounts.patientInviteCode(personal)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(CareRelationErrorCode.SELF_RELATION_NOT_ALLOWED);
    }

    @Test
    void withdrawalRemovesEveryLinkedAccountAndTheirRelations() {
        UserAccountService accounts = new UserAccountService(users, patients);
        AccountSwitchService switches = new AccountSwitchService(users, accounts);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users);
        UserWithdrawalService withdrawals = new UserWithdrawalService(users, patients, relations);
        User personal = accounts.chooseRole(accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(), UserRole.PATIENT);
        User myCaregiver = switches.switchTo("kakao-soonja", UserRole.CAREGIVER);
        User grandma = accounts.chooseRole(accounts.loginOrRegister("kakao-grandma", "박할머니").getKakaoId(), UserRole.PATIENT);
        User daughter = accounts.chooseRole(accounts.loginOrRegister("kakao-daughter", "김딸").getKakaoId(), UserRole.CAREGIVER);
        CareRelation caringGrandma = cares.accept(grandma, cares.request(myCaregiver, accounts.patientInviteCode(grandma)).getId());
        CareRelation caredByDaughter = cares.accept(personal, cares.request(daughter, accounts.patientInviteCode(personal)).getId());

        var result = withdrawals.withdraw(myCaregiver.getKakaoId(), true);

        assertThat(result.revokedRelationCount()).isEqualTo(2);
        assertThat(personal.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(myCaregiver.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(personal.getKakaoId()).isNull();
        assertThat(myCaregiver.getKakaoId()).isNull();
        assertThat(caringGrandma.getStatus()).isEqualTo(CareRelationStatus.REVOKED);
        assertThat(caredByDaughter.getStatus()).isEqualTo(CareRelationStatus.REVOKED);
        assertThat(grandma.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(daughter.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
