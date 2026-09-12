package com.gyote.silvercare.user.command.application;

import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserAccountServiceTest {

    @Autowired
    private UserRepository users;

    @Test
    void firstLoginCreatesAccountWithoutRoleEmailOrPassword() {
        UserAccountService accounts = new UserAccountService(users);

        User created = accounts.loginOrRegister("kakao-soonja", "김순자");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getKakaoId()).isEqualTo("kakao-soonja");
        assertThat(created.getName()).isEqualTo("김순자");
        assertThat(created.getRole()).isEqualTo(UserRole.PENDING);
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.getEmail()).isNull();
        assertThat(created.getPasswordHash()).isNull();
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void secondLoginKeepsOneRowAndUpdatesName() {
        UserAccountService accounts = new UserAccountService(users);
        accounts.loginOrRegister("kakao-minji", "민지");

        User again = accounts.loginOrRegister("kakao-minji", "김민지");

        assertThat(users.count()).isEqualTo(1);
        assertThat(again.getName()).isEqualTo("김민지");
        assertThat(again.getRole()).isEqualTo(UserRole.PENDING);
    }

    @Test
    void chooseRoleOnceAsPatient() {
        UserAccountService accounts = new UserAccountService(users);
        accounts.loginOrRegister("kakao-soonja", "김순자");

        User chosen = accounts.chooseRole("kakao-soonja", UserRole.PATIENT);

        assertThat(chosen.getRole()).isEqualTo(UserRole.PATIENT);
        assertThat(chosen.getInviteCode()).isNotBlank();
        assertThat(chosen.getInviteCode()).hasSize(6);
        assertThatThrownBy(() -> accounts.chooseRole("kakao-soonja", UserRole.CAREGIVER))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blankKakaoIdIsRejected() {
        UserAccountService accounts = new UserAccountService(users);

        assertThatThrownBy(() -> accounts.loginOrRegister("  ", "민지"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void demoCaregiverIsCreatedOnce() {
        UserAccountService accounts = new UserAccountService(users);

        User first = accounts.ensureDemoUser(UserRole.CAREGIVER);
        User again = accounts.ensureDemoUser(UserRole.CAREGIVER);

        assertThat(first.getKakaoId()).isEqualTo("demo-caregiver");
        assertThat(first.getName()).isEqualTo("김민지");
        assertThat(first.getRole()).isEqualTo(UserRole.CAREGIVER);
        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(users.count()).isEqualTo(1);
    }
}
