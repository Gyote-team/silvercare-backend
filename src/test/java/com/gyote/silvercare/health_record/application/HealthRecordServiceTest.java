package com.gyote.silvercare.health_record.application;

import com.gyote.silvercare.health_record.command.application.HealthRecordCommandService;
import com.gyote.silvercare.health_record.domain.HealthRecordAccessPolicy;
import com.gyote.silvercare.health_record.domain.repository.HealthRecordRepository;
import com.gyote.silvercare.health_record.error.HealthRecordErrorCode;
import com.gyote.silvercare.health_record.query.application.HealthRecordQueryService;
import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.care_relation.domain.repository.CareRelationRepository;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.*;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.command.application.*;
import com.gyote.silvercare.global.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql=false)
class HealthRecordServiceTest {
    @Autowired UserRepository users;
    @Autowired PatientRepository patients;
    @Autowired CareRelationRepository relations;
    @Autowired HealthRecordRepository records;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    UserAccountService accounts;
    AccountSwitchService switches;
    CareRelationCommandService cares;
    HealthRecordCommandService commands;
    HealthRecordQueryService queries;
    User personal;
    User caregiver;
    UUID patientId;

    @BeforeEach
    void setup() {
        accounts=new UserAccountService(users,patients);
        switches=new AccountSwitchService(users,accounts);
        cares=new CareRelationCommandService(relations,patients,users);
        var access=new HealthRecordAccessPolicy(patients,relations);
        commands=new HealthRecordCommandService(records,access,jdbc);
        queries=new HealthRecordQueryService(records,access,users);
        personal=accounts.chooseRole(accounts.loginOrRegister("personal","개인").getKakaoId(),UserRole.PATIENT);
        caregiver=accounts.chooseRole(accounts.loginOrRegister("caregiver","보호자").getKakaoId(),UserRole.CAREGIVER);
        patientId=patients.findByUserId(personal.getId()).orElseThrow().getId();
    }
    UUID connect() {
        return cares.accept(personal,cares.request(caregiver,accounts.patientInviteCode(personal)).getId()).getId();
    }
    void assertCode(Runnable action, HealthRecordErrorCode expected) {
        assertThatThrownBy(action::run).isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException)e).getErrorCode()).isEqualTo(expected);
    }
    @Test void personalCanCreateWithoutCaregiverAndSurvivesSwitching() {
        var record=commands.create(personal,null,"본인 기록",null);
        var caringSelf=switches.switchTo(personal.getKakaoId(),UserRole.CAREGIVER);
        assertCode(() -> queries.detail(caringSelf,record.getId()),HealthRecordErrorCode.ACCESS_DENIED);
        var back=switches.switchTo(caringSelf.getKakaoId(),UserRole.PATIENT);
        assertThat(queries.detail(back,record.getId()).body()).isEqualTo("본인 기록");
        assertThat(records.count()).isEqualTo(1);
    }
    @Test void pendingAndRevokedCaregiverCannotReadOrWrite() {
        var record=commands.create(personal,null,"본인 기록",null);
        var relation=cares.request(caregiver,accounts.patientInviteCode(personal));
        assertCode(() -> commands.create(caregiver,patientId,"금지",null),HealthRecordErrorCode.ACCESS_DENIED);
        assertCode(() -> queries.detail(caregiver,record.getId()),HealthRecordErrorCode.ACCESS_DENIED);
        cares.accept(personal,relation.getId());
        assertThat(queries.list(caregiver,patientId,null,20).items()).hasSize(1);
        cares.revoke(personal,relation.getId());
        assertCode(() -> queries.detail(caregiver,record.getId()),HealthRecordErrorCode.ACCESS_DENIED);
        assertThat(queries.detail(personal,record.getId()).body()).isEqualTo("본인 기록");
    }
    @Test void personalMayManageCaregiverRecordsEvenAfterUnlinking() {
        connect();
        var record=commands.create(caregiver,patientId,"대리 작성",null);
        assertThat(queries.detail(personal,record.getId()).proxyWritten()).isTrue();
        commands.update(caregiver,record.getId(),"보호자 수정",null);
        cares.revoke(personal,relations.findByPatientIdAndStatus(patientId,
                com.gyote.silvercare.care_relation.domain.CareRelationStatus.ACTIVE).get(0).getId());
        commands.update(personal,record.getId(),"수정 완료",null);
        assertThat(queries.detail(personal,record.getId()).body()).isEqualTo("수정 완료");
        assertThat(queries.detail(personal,record.getId()).authorUserId()).isEqualTo(caregiver.getId());
        commands.delete(personal,record.getId());
        em.flush();
        assertThat(records.findById(record.getId()).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(queries.list(personal,null,null,20).items()).isEmpty();
        assertCode(() -> queries.detail(personal,record.getId()),HealthRecordErrorCode.NOT_FOUND);
    }
    @Test void revokedAuthorCannotChangePreviouslyWrittenRecords() {
        UUID relation=connect();
        var record=commands.create(caregiver,patientId,"대리 작성",null);
        cares.revoke(personal,relation);
        assertCode(() -> commands.update(caregiver,record.getId(),"수정",null),HealthRecordErrorCode.ACCESS_DENIED);
        assertCode(() -> commands.delete(caregiver,record.getId()),HealthRecordErrorCode.ACCESS_DENIED);
        assertThat(queries.detail(personal,record.getId()).body()).isEqualTo("대리 작성");
    }
    @Test void differentPatientRecordsAreIsolated() {
        connect();
        User other=accounts.chooseRole(accounts.loginOrRegister("other","다른 개인").getKakaoId(),UserRole.PATIENT);
        UUID otherId=patients.findByUserId(other.getId()).orElseThrow().getId();
        var record=commands.create(other,null,"다른 개인 기록",null);
        assertThat(queries.list(caregiver,patientId,null,20).items()).isEmpty();
        assertCode(() -> queries.detail(caregiver,record.getId()),HealthRecordErrorCode.ACCESS_DENIED);
        assertCode(() -> queries.list(personal,otherId,null,20),HealthRecordErrorCode.ACCESS_DENIED);
    }
    @Test void validatesBodyAndSizeBoundaries() {
        assertCode(() -> commands.create(personal,null," ",null),HealthRecordErrorCode.INVALID_REQUEST);
        assertCode(() -> commands.create(personal,null,"가".repeat(2001),null),HealthRecordErrorCode.INVALID_REQUEST);
        commands.create(personal,null,"가".repeat(2000),null);
        commands.create(personal,null,"가",null);
        assertThat(queries.list(personal,null,null,null).items()).hasSize(2);
        assertCode(() -> queries.list(personal,null,null,0),HealthRecordErrorCode.INVALID_REQUEST);
        assertCode(() -> queries.list(personal,null,null,101),HealthRecordErrorCode.INVALID_REQUEST);
        assertCode(() -> queries.list(caregiver,null,null,20),HealthRecordErrorCode.INVALID_REQUEST);
    }
    @Test void cursorHandlesEqualTimestampsAndExcludesDeletedRecords() {
        var first=commands.create(personal,null,"1",null);
        var second=commands.create(personal,null,"2",null);
        var third=commands.create(personal,null,"3",null);
        var deleted=commands.create(personal,null,"삭제",null);
        commands.delete(personal,deleted.getId());
        em.flush();
        jdbc.update("update health_records set created_at=TIMESTAMP WITH TIME ZONE '2026-10-08 00:00:00+00'");
        em.clear();
        var page1=queries.list(personal,null,null,2);
        assertThat(page1.items()).hasSize(2);
        assertThat(page1.hasNext()).isTrue();
        var page2=queries.list(personal,null,page1.nextCursor(),2);
        assertThat(page2.items()).hasSize(1);
        assertThat(page2.hasNext()).isFalse();
        var ids=new java.util.ArrayList<UUID>();
        page1.items().forEach(r -> ids.add(r.recordId())); page2.items().forEach(r -> ids.add(r.recordId()));
        assertThat(ids).containsExactlyInAnyOrder(first.getId(),second.getId(),third.getId());
    }
    @Test void rejectsMalformedAndOtherPatientCursors() {
        commands.create(personal,null,"1",null); commands.create(personal,null,"2",null);
        String cursor=queries.list(personal,null,null,1).nextCursor();
        assertCode(() -> queries.list(personal,null,"bad",20),HealthRecordErrorCode.INVALID_REQUEST);
        User other=accounts.chooseRole(accounts.loginOrRegister("other","다른 개인").getKakaoId(),UserRole.PATIENT);
        assertCode(() -> queries.list(other,null,cursor,20),HealthRecordErrorCode.INVALID_REQUEST);
    }
    @Test void switchingFromCaregiverCreatesIndependentPersonalProfile() {
        connect();
        commands.create(caregiver,patientId,"가족 기록",null);
        var own=switches.switchTo(caregiver.getKakaoId(),UserRole.PATIENT);
        assertThat(queries.list(own,null,null,20).items()).isEmpty();
        commands.create(own,null,"내 기록",null);
        var back=switches.switchTo(own.getKakaoId(),UserRole.CAREGIVER);
        assertThat(queries.list(back,patientId,null,20).items()).extracting(r -> r.body()).containsExactly("가족 기록");
        assertThat(queries.list(own,null,null,20).items()).extracting(r -> r.body()).containsExactly("내 기록");
    }
    @Test void visitMustBelongToTargetAndBeActive() {
        JdbcTemplate visitLookup=org.mockito.Mockito.mock(JdbcTemplate.class);
        var commandWithVisits=new HealthRecordCommandService(records,new HealthRecordAccessPolicy(patients,relations),visitLookup);
        UUID valid=UUID.randomUUID(), wrong=UUID.randomUUID(), canceled=UUID.randomUUID();
        String sql="select count(*) from visits where id=? and patient_id=? and deleted_at is null and status not in ('CANCELLED','CANCELED')";
        org.mockito.Mockito.when(visitLookup.queryForObject(sql,Integer.class,valid,patientId)).thenReturn(1);
        org.mockito.Mockito.when(visitLookup.queryForObject(sql,Integer.class,wrong,patientId)).thenReturn(0);
        org.mockito.Mockito.when(visitLookup.queryForObject(sql,Integer.class,canceled,patientId)).thenReturn(0);
        assertThat(commandWithVisits.create(personal,null,"방문 기록",valid).getVisitId()).isEqualTo(valid);
        assertCode(() -> commandWithVisits.create(personal,null,"금지",wrong),HealthRecordErrorCode.INVALID_VISIT);
        assertCode(() -> commandWithVisits.create(personal,null,"금지",canceled),HealthRecordErrorCode.INVALID_VISIT);
        assertCode(() -> commandWithVisits.create(personal,null,"금지",UUID.randomUUID()),HealthRecordErrorCode.INVALID_VISIT);
    }
}
