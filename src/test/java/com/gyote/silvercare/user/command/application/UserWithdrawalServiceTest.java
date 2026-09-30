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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserWithdrawalServiceTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private CareRelationRepository relations;

    @Test
    void patientWithdrawalRevokesOnlyItsActiveRelationsUsingPatientProfileId() {
        User patientUser = saveUser("patient-kakao", UserRole.PATIENT);
        Patient patient = savePatient(patientUser, "ABC123");
        User caregiver = saveUser("caregiver-kakao", UserRole.CAREGIVER);
        CareRelation active = saveRelation(patient.getId(), caregiver, CareRelationStatus.ACTIVE);
        CareRelation requested = saveRelation(patient.getId(), saveUser("other-kakao", UserRole.CAREGIVER), CareRelationStatus.REQUESTED);

        WithdrawalResponse response = service().withdraw(patientUser.getKakaoId(), true);

        assertThat(response.userStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(response.revokedRelationCount()).isEqualTo(1);
        assertThat(patientUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(patientUser.getDeletedAt()).isNotNull();
        assertThat(active.getStatus()).isEqualTo(CareRelationStatus.REVOKED);
        assertThat(active.getEndedAt()).isNotNull();
        assertThat(requested.getStatus()).isEqualTo(CareRelationStatus.REQUESTED);
    }

    @Test
    void caregiverWithdrawalRevokesOnlyActiveRelations() {
        User patientUser = saveUser("patient-kakao", UserRole.PATIENT);
        Patient patient = savePatient(patientUser, "ABC123");
        User caregiver = saveUser("caregiver-kakao", UserRole.CAREGIVER);
        CareRelation active = saveRelation(patient.getId(), caregiver, CareRelationStatus.ACTIVE);

        WithdrawalResponse response = service().withdraw(caregiver.getKakaoId(), true);

        assertThat(response.revokedRelationCount()).isEqualTo(1);
        assertThat(caregiver.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(active.getStatus()).isEqualTo(CareRelationStatus.REVOKED);
    }

    @Test
    void withdrawalRequiresExplicitConfirmation() {
        User user = saveUser("patient-kakao", UserRole.PATIENT);

        assertThatThrownBy(() -> service().withdraw(user.getKakaoId(), false))
                .isInstanceOf(BusinessException.class);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    private UserWithdrawalService service() {
        return new UserWithdrawalService(users, patients, relations);
    }

    private User saveUser(String kakaoId, UserRole role) {
        User user = new User();
        user.setKakaoId(kakaoId);
        user.setName(kakaoId);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(user);
    }

    private Patient savePatient(User user, String inviteCode) {
        Patient patient = new Patient();
        patient.setUserId(user.getId());
        patient.setInviteCode(inviteCode);
        return patients.saveAndFlush(patient);
    }

    private CareRelation saveRelation(java.util.UUID patientId, User caregiver, CareRelationStatus status) {
        CareRelation relation = new CareRelation();
        relation.setPatientId(patientId);
        relation.setCaregiverId(caregiver.getId());
        relation.setStatus(status);
        return relations.saveAndFlush(relation);
    }
}
