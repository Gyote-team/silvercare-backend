package com.gyote.silvercare.health_record.application;

import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.domain.CareRelationCode;
import com.gyote.silvercare.health_record.command.application.HealthRecordCommand;
import com.gyote.silvercare.health_record.command.application.HealthRecordCommandService;
import com.gyote.silvercare.health_record.domain.HealthRecordAccessPolicy;
import com.gyote.silvercare.health_record.domain.HealthRecordInputType;
import com.gyote.silvercare.health_record.domain.repository.HealthRecordRepository;
import com.gyote.silvercare.health_record.query.application.HealthRecordQueryService;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.query.application.UserQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class HealthRecordServiceTest {

    @Autowired private UserRepository users;
    @Autowired private PatientRepository patients;
    @Autowired private com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository relations;
    @Autowired private HealthRecordRepository records;

    @Test
    void activeCaregiverWritesAndReadsOnlySelectedPersonsRecord() {
        UserAccountService accounts = new UserAccountService(users, patients);
        User minji = register(accounts, "health-minji", "김민지", UserRole.CAREGIVER);
        User soonja = register(accounts, "health-soonja", "김순자", UserRole.PATIENT);
        User youngsoo = register(accounts, "health-youngsoo", "박영수", UserRole.PATIENT);
        CareRelationCommandService careCommands = new CareRelationCommandService(relations, patients, users);
        var request = careCommands.request(minji, CareRelationCode.display(accounts.patientInviteCode(soonja)));
        careCommands.accept(soonja, request.getId());

        HealthRecordAccessPolicy access = new HealthRecordAccessPolicy(patients, relations);
        HealthRecordCommandService commands = new HealthRecordCommandService(records, access, users);
        HealthRecordQueryService queries = new HealthRecordQueryService(
                records, access, users, new UserQueryService(users));
        UUID soonjaPatientId = patients.findByUserId(soonja.getId()).orElseThrow().getId();
        UUID youngsooPatientId = patients.findByUserId(youngsoo.getId()).orElseThrow().getId();

        commands.create(minji, command(soonjaPatientId, "soonja-first"));
        commands.create(youngsoo, command(youngsooPatientId, "youngsoo-first"));

        var viewed = queries.list(minji, soonjaPatientId, 20);

        assertThat(viewed).hasSize(1);
        assertThat(viewed.get(0).patientId()).isEqualTo(soonjaPatientId);
        assertThat(viewed.get(0).content()).isEqualTo("soonja-first");
        assertThat(viewed.get(0).authorUserId()).isEqualTo(minji.getId());
        assertThat(viewed.get(0).proxyWritten()).isTrue();
        assertThatThrownBy(() -> queries.list(minji, youngsooPatientId, 20)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void repeatedIdempotencyKeyReturnsOriginalRecord() {
        UserAccountService accounts = new UserAccountService(users, patients);
        User minji = register(accounts, "idempotency-minji", "김민지", UserRole.CAREGIVER);
        UUID patientId = patients.findByUserId(minji.getId()).orElseThrow().getId();
        HealthRecordAccessPolicy access = new HealthRecordAccessPolicy(patients, relations);
        HealthRecordCommandService commands = new HealthRecordCommandService(records, access, users);
        HealthRecordCommand command = command(patientId, "same request");

        var first = commands.create(minji, command);
        var second = commands.create(minji, command);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(records.count()).isEqualTo(1);
    }

    private static HealthRecordCommand command(UUID patientId, String content) {
        return new HealthRecordCommand(patientId, null, HealthRecordInputType.TEXT, content, null, "request-key-001");
    }

    private static User register(UserAccountService accounts, String kakaoId, String name, UserRole role) {
        accounts.loginOrRegister(kakaoId, name);
        return accounts.chooseRole(kakaoId, role);
    }
}
