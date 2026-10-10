package com.gyote.silvercare.notification;
import com.gyote.silvercare.notification.domain.repository.SystemNotificationRepository;
import com.gyote.silvercare.notification.command.application.SystemNotificationCommandService;
import com.gyote.silvercare.notification.query.application.SystemNotificationQueryService;
import com.gyote.silvercare.user.command.application.*;
import com.gyote.silvercare.user.domain.*;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.health_record.command.application.HealthRecordCommandService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import com.gyote.silvercare.global.auth.application.JwtService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
@SpringBootTest @Transactional @AutoConfigureMockMvc
class SystemNotificationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired UserAccountService accounts;
    @Autowired AccountSwitchService switches;
    @Autowired CareRelationCommandService cares;
    @Autowired HealthRecordCommandService records;
    @Autowired PatientRepository patients;
    @Autowired SystemNotificationRepository notifications;
    @Autowired SystemNotificationCommandService commands;
    @Autowired SystemNotificationQueryService queries;
    User personal,caregiver;
    @BeforeEach void setup() {
        String key=java.util.UUID.randomUUID().toString();
        personal=accounts.chooseRole(accounts.loginOrRegister("np-"+key,"개인").getKakaoId(),UserRole.PATIENT);
        caregiver=accounts.chooseRole(accounts.loginOrRegister("nc-"+key,"보호자").getKakaoId(),UserRole.CAREGIVER);
    }
    @Test void missingOrOtherUsersNoticeUsesCommonErrorResponse() throws Exception {
        cares.request(caregiver,accounts.patientInviteCode(personal));
        var notice=queries.list(personal.getId()).items().get(0);
        for (var id : java.util.List.of(notice.id(),java.util.UUID.randomUUID())) {
            mvc.perform(patch("/api/notifications/"+id+"/read")
                    .header("Authorization","Bearer "+jwt.create(caregiver)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SYSTEM_NOTIFICATION_001"))
                    .andExpect(jsonPath("$.message").value("알림을 찾을 수 없습니다."))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
        assertThat(queries.list(personal.getId()).unreadCount()).isEqualTo(1);
        mvc.perform(patch("/api/notifications/"+notice.id()+"/read")
                .header("Authorization","Bearer "+jwt.create(personal)))
                .andExpect(status().isNoContent());
    }
    @Test void relationEventsNotifyOtherParticipantAndReadsAreScoped() {
        var relation=cares.request(caregiver,accounts.patientInviteCode(personal));
        var page=queries.list(personal.getId());
        assertThat(page.unreadCount()).isEqualTo(1);
        assertThat(page.items().get(0).type()).isEqualTo("RELATION_REQUESTED");
        assertThatThrownBy(()->commands.read(caregiver.getId(),page.items().get(0).id()))
            .isInstanceOf(com.gyote.silvercare.global.exception.BusinessException.class);
        commands.read(personal.getId(),page.items().get(0).id());
        commands.read(personal.getId(),page.items().get(0).id());
        assertThat(queries.list(personal.getId()).unreadCount()).isZero();
        cares.accept(personal,relation.getId());
        assertThat(queries.list(caregiver.getId()).items()).extracting(i->i.type()).containsExactly("RELATION_ACCEPTED");
        cares.revoke(personal,relation.getId());
        assertThat(queries.list(caregiver.getId()).unreadCount()).isEqualTo(2);
        commands.readAll(personal.getId());
        assertThat(queries.list(caregiver.getId()).unreadCount()).isEqualTo(2);
        commands.readAll(caregiver.getId());
        assertThat(queries.list(caregiver.getId()).unreadCount()).isZero();
    }
    @Test void caregiverRecordNotifiesPersonalWithoutCopyingMedicalBody() {
        var relation=cares.request(caregiver,accounts.patientInviteCode(personal));
        cares.accept(personal,relation.getId());
        var patientId=patients.findByUserId(personal.getId()).orElseThrow().getId();
        records.create(caregiver,patientId,"비공개 건강기록 본문",null);
        var notice=queries.list(personal.getId()).items().stream().filter(i->i.type().equals("HEALTH_RECORD_CREATED")).findFirst().orElseThrow();
        assertThat(notice.message()).contains("보호자").doesNotContain("비공개 건강기록 본문");
        var linked=switches.switchTo(personal.getKakaoId(),UserRole.CAREGIVER);
        assertThat(queries.list(linked.getId()).items()).isEmpty();
        long count=notifications.count();
        records.create(personal,null,"본인 기록",null);
        assertThat(notifications.count()).isEqualTo(count);
        cares.revoke(personal,relation.getId());
        count=notifications.count();
        assertThatThrownBy(()->records.create(caregiver,patientId,"차단",null)).isInstanceOf(com.gyote.silvercare.global.exception.BusinessException.class);
        assertThat(notifications.count()).isEqualTo(count);
    }
}
