package com.gyote.silvercare.action_item;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.gyote.silvercare.action_item.command.application.ActionItemCommandService;
import com.gyote.silvercare.global.auth.application.JwtService;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.command.application.MedicalDocumentCommandService;
import com.gyote.silvercare.medical_document.domain.entity.MedicalDocument;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.schedule.domain.RepeatFrequency;
import com.gyote.silvercare.schedule.domain.ScheduleInput;
import com.gyote.silvercare.schedule.domain.ScheduleType;
import com.gyote.silvercare.schedule.domain.entity.Schedule;
import com.gyote.silvercare.schedule.query.application.ScheduleQueryService;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.visit.domain.VisitStatus;
import com.gyote.silvercare.visit.domain.entity.Visit;
import com.gyote.silvercare.visit.domain.repository.VisitRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** 실제 보안·트랜잭션·행 잠금·일정 API를 함께 검증한다. 동시성 검증을 위해 테스트 트랜잭션을 사용하지 않는다. */
@SpringBootTest(
        properties =
                "spring.datasource.url=jdbc:h2:mem:action-schedule-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ActionItemScheduleApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired PatientRepository patients;
    @Autowired VisitRepository visits;
    @Autowired MedicalDocumentRepository documents;
    @Autowired MedicalDocumentCommandService documentCommands;
    @Autowired ActionItemCommandService commands;
    @Autowired JdbcTemplate db;
    @Autowired com.gyote.silvercare.schedule.domain.repository.ScheduleRepository schedules;
    User personal, caregiver, other;
    UUID patient, candidate;
    MedicalDocument document;

    @BeforeEach
    void setup() {
        db.execute(
                "create table if not exists document_pages(id uuid primary key,document_id"
                        + " uuid,page_no int)");
        db.execute(
                "create table if not exists extracted_items(id uuid primary key,analysis_id"
                        + " uuid,page_id uuid,source_text text)");
        personal = user(UserRole.PATIENT);
        caregiver = user(UserRole.CAREGIVER);
        other = user(UserRole.PATIENT);
        patient = patients.findByUserId(personal.getId()).orElseThrow().getId();
        Visit visit =
                visits.saveAndFlush(
                        Visit.create(
                                patient,
                                LocalDate.of(2026, 11, 27),
                                "가상 병원",
                                "내과",
                                null,
                                VisitStatus.SCHEDULED));
        document =
                documents.saveAndFlush(
                        MedicalDocument.uploaded(
                                patient,
                                visit.getId(),
                                personal.getId(),
                                "가상 처방전.pdf",
                                "synthetic/file.pdf",
                                "application/pdf",
                                10,
                                "test"));
        candidate = UUID.randomUUID();
        db.update(
                "insert into"
                    + " action_items(id,visit_id,document_id,type,title,approval_status,created_at)"
                    + " values (?,?,?,'MEDICATION','가상 복약','PENDING',CURRENT_TIMESTAMP)",
                candidate,
                visit.getId(),
                document.getId());
    }

    @Test
    void approvalCreatesAllSlotsOnceAndRecordsActor() throws Exception {
        JsonNode response =
                body(
                        call(
                                        personal,
                                        post("/api/action-items/" + candidate + "/approve")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(approval()))
                                .andExpect(status().isOk()));
        assertThat(response.path("scheduleIds").size()).isEqualTo(2);
        assertThat(state()).isEqualTo("APPROVED");
        assertThat(
                        db.queryForObject(
                                "select approved_by_user_id from action_items where id=?",
                                UUID.class,
                                candidate))
                .isEqualTo(personal.getId());
        call(
                        personal,
                        post("/api/action-items/" + candidate + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(approval()))
                .andExpect(status().isConflict());
        call(personal, post("/api/action-items/" + candidate + "/reject"))
                .andExpect(status().isConflict());
        assertThat(scheduleCount()).isEqualTo(2);
        String schedule = response.path("scheduleIds").get(0).asText();
        call(personal, get("/api/schedules/" + schedule))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repeatFrequency").value("DAILY"));
    }

    @Test
    void rejectionIsTerminalAndCreatesNoSchedule() throws Exception {
        call(personal, post("/api/action-items/" + candidate + "/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("REJECTED"));
        call(
                        personal,
                        post("/api/action-items/" + candidate + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(approval()))
                .andExpect(status().isConflict());
        assertThat(scheduleCount()).isZero();
    }

    @Test
    void invalidSecondSlotLeavesNoPartialApproval() throws Exception {
        var first = input("아침", "2026-12-01T00:00:00Z");
        var invalid = input("저녁", "2026-12-01T10:00:00Z");
        invalid.put("repeatUntil", "2026-11-01");
        call(
                        personal,
                        post("/api/action-items/" + candidate + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of("schedules", List.of(first, invalid)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        assertThat(state()).isEqualTo("PENDING");
        assertThat(scheduleCount()).isZero();
    }

    @Test
    void simultaneousApprovalsHaveOneWinner() throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> run =
                    () -> {
                        start.await();
                        return call(
                                        personal,
                                        post("/api/action-items/" + candidate + "/approve")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(approval()))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    };
            var a = pool.submit(run);
            var b = pool.submit(run);
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(scheduleCount()).isEqualTo(2);
            assertThat(state()).isEqualTo("APPROVED");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void databaseWriteFailureRollsBackApprovalAndAllNewSchedules() {
        schedules.saveAndFlush(Schedule.create(patient, candidate, 0, domainInput()));
        assertThatThrownBy(() -> commands.approve(personal, candidate, List.of(domainInput())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(state()).isEqualTo("PENDING");
        assertThat(scheduleCount()).isEqualTo(1);
    }

    @Test
    void sourceDeletionAndApprovalAreSerialized() throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var approved =
                    pool.submit(
                            () -> {
                                start.await();
                                try {
                                    commands.approve(personal, candidate, List.of(domainInput()));
                                    return true;
                                } catch (BusinessException e) {
                                    assertThat(e.getErrorCode().code())
                                            .isEqualTo("ACTION_ITEM_003");
                                    return false;
                                }
                            });
            var deleted =
                    pool.submit(
                            () -> {
                                start.await();
                                documentCommands.delete(personal, document.getId());
                                return true;
                            });
            start.countDown();
            boolean won = approved.get(15, TimeUnit.SECONDS);
            assertThat(deleted.get(15, TimeUnit.SECONDS)).isTrue();
            assertThat(state()).isEqualTo(won ? "APPROVED" : "CANCELED");
            assertThat(scheduleCount()).isEqualTo(won ? 1 : 0);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void approvedSchedulesSurviveSourceDeletionAndTheirOwnDeletionDoesNotReopenCandidate()
            throws Exception {
        var ids = commands.approve(personal, candidate, List.of(domainInput()));
        documentCommands.delete(personal, document.getId());
        call(personal, get("/api/schedules/" + ids.get(0))).andExpect(status().isOk());
        call(personal, delete("/api/schedules/" + ids.get(0)).param("version", "0"))
                .andExpect(status().isNoContent());
        call(personal, get("/api/schedules/" + ids.get(0))).andExpect(status().isNotFound());
        assertThat(state()).isEqualTo("APPROVED");
        call(
                        personal,
                        post("/api/action-items/" + candidate + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(approval()))
                .andExpect(status().isConflict());
    }

    @Test
    void manualCrudWorksWithoutGuardianAndDetectsStaleVersion() throws Exception {
        var input = input("수동 복약", "2026-12-01T00:00:00Z");
        JsonNode created =
                body(
                        call(
                                        personal,
                                        post("/api/schedules")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(json.writeValueAsString(input)))
                                .andExpect(status().isCreated()));
        String id = created.path("scheduleId").asText();
        assertThat(created.path("actionItemId").isNull()).isTrue();
        input.put("title", "변경한 복약");
        input.put("status", "COMPLETED");
        input.put("version", 0);
        call(
                        personal,
                        put("/api/schedules/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        call(
                        personal,
                        put("/api/schedules/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(input)))
                .andExpect(status().isConflict());
        call(personal, delete("/api/schedules/" + id).param("version", "0"))
                .andExpect(status().isConflict());
        call(personal, get("/api/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
        call(personal, delete("/api/schedules/" + id).param("version", "1"))
                .andExpect(status().isNoContent());
        call(personal, get("/api/schedules")).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void currentGuardianAccessIsRequiredForApprovalAndAllScheduleOperations() throws Exception {
        call(
                        caregiver,
                        post("/api/action-items/" + candidate + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(approval()))
                .andExpect(status().isForbidden());
        UUID link = UUID.randomUUID();
        db.update(
                "insert into caregiver_links(id,patient_id,caregiver_user_id,status,requested_at)"
                        + " values (?,?,?,'ACTIVE',CURRENT_TIMESTAMP)",
                link,
                patient,
                caregiver.getId());
        JsonNode response =
                body(
                        call(
                                        caregiver,
                                        post("/api/action-items/" + candidate + "/approve")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(approval()))
                                .andExpect(status().isOk()));
        String id = response.path("scheduleIds").get(0).asText();
        call(caregiver, get("/api/schedules").param("patientId", patient.toString()))
                .andExpect(status().isOk());
        call(other, get("/api/schedules/" + id)).andExpect(status().isForbidden());
        db.update("update caregiver_links set status='REVOKED' where id=?", link);
        call(caregiver, get("/api/schedules/" + id)).andExpect(status().isForbidden());
        call(caregiver, delete("/api/schedules/" + id).param("version", "0"))
                .andExpect(status().isForbidden());
        call(caregiver, get("/api/schedules")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "REJECTED", "CANCELED"})
    void finishedCandidatesCannotBeProcessedAgain(String state) throws Exception {
        db.update("update action_items set approval_status=? where id=?", state, candidate);
        call(personal, post("/api/action-items/" + candidate + "/reject"))
                .andExpect(status().isConflict());
    }

    @Test
    void canceledVisitCannotBeApproved() throws Exception {
        db.update("update visits set status='CANCELED' where id=?", document.getVisitId());
        call(
                        personal,
                        post("/api/action-items/" + candidate + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(approval()))
                .andExpect(status().isConflict());
        assertThat(scheduleCount()).isZero();
    }

    @Test
    void repeatedRulesValidateTypeIntervalEndAndDuplicateTimes() throws Exception {
        for (var bad : List.of("type", "interval", "until", "end", "duplicates")) {
            var input = input("복약", "2026-12-01T00:00:00Z");
            switch (bad) {
                case "type" -> input.put("scheduleType", "REVISIT");
                case "interval" -> input.put("repeatInterval", 0);
                case "until" -> input.put("repeatUntil", "2030-01-01");
                case "end" -> input.put("endsAt", "2026-11-30T00:00:00Z");
            }
            var values = bad.equals("duplicates") ? List.of(input, input) : List.of(input);
            call(
                            personal,
                            post("/api/action-items/" + candidate + "/approve")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json.writeValueAsString(Map.of("schedules", values))))
                    .andExpect(status().isBadRequest());
        }
        assertThat(state()).isEqualTo("PENDING");
        assertThat(scheduleCount()).isZero();
    }

    @Test
    void recurrenceKeepsKoreanTimeAndIncludesLastDate() {
        var daily = Schedule.create(patient, null, 0, domainInput());
        assertThat(
                        ScheduleQueryService.nextOccurrence(
                                daily, Instant.parse("2026-12-01T00:00:01Z")))
                .isEqualTo(Instant.parse("2026-12-02T00:00:00Z"));
        assertThat(
                        ScheduleQueryService.nextOccurrence(
                                daily, Instant.parse("2026-12-07T00:00:00Z")))
                .isEqualTo(Instant.parse("2026-12-07T00:00:00Z"));
        assertThat(
                        ScheduleQueryService.nextOccurrence(
                                daily, Instant.parse("2026-12-07T00:00:01Z")))
                .isNull();
        var weekly =
                Schedule.create(
                        patient,
                        null,
                        0,
                        new ScheduleInput(
                                "주간 복약",
                                ScheduleType.MEDICATION,
                                Instant.parse("2026-12-01T00:00:00Z"),
                                null,
                                RepeatFrequency.WEEKLY,
                                2,
                                LocalDate.parse("2026-12-31")));
        assertThat(
                        ScheduleQueryService.nextOccurrence(
                                weekly, Instant.parse("2026-12-02T00:00:00Z")))
                .isEqualTo(Instant.parse("2026-12-15T00:00:00Z"));
    }

    @Test
    void invalidVersionAndPatientReassignmentUseCommonInputError() throws Exception {
        UUID id = commands.approve(personal, candidate, List.of(domainInput())).get(0);
        call(personal, delete("/api/schedules/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
        var input = input("복약", "2026-12-01T00:00:00Z");
        input.put("version", 0);
        input.put("status", "SCHEDULED");
        input.put("patientId", patients.findByUserId(other.getId()).orElseThrow().getId());
        call(
                        personal,
                        put("/api/schedules/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
        call(personal, get("/api/schedules/" + id))
                .andExpect(jsonPath("$.patientId").value(patient.toString()));
    }

    @Test
    void extremeTimestampIsAnInputErrorRatherThanServerFailure() throws Exception {
        var input = input("복약", "+999999999-12-31T23:59:59Z");
        call(
                        personal,
                        post("/api/schedules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(input)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"));
    }

    private User user(UserRole role) {
        var u = accounts.loginOrRegister("schedule-test-" + UUID.randomUUID(), "가상 사용자");
        return accounts.chooseRole(u.getKakaoId(), role);
    }

    private Map<String, Object> input(String title, String start) {
        return new HashMap<>(
                Map.of(
                        "title",
                        title,
                        "scheduleType",
                        "MEDICATION",
                        "startsAt",
                        start,
                        "repeatFrequency",
                        "DAILY",
                        "repeatInterval",
                        1,
                        "repeatUntil",
                        "2026-12-07"));
    }

    private ScheduleInput domainInput() {
        return new ScheduleInput(
                "가상 복약",
                ScheduleType.MEDICATION,
                Instant.parse("2026-12-01T00:00:00Z"),
                null,
                RepeatFrequency.DAILY,
                1,
                LocalDate.parse("2026-12-07"));
    }

    private String approval() throws Exception {
        return json.writeValueAsString(
                Map.of(
                        "schedules",
                        List.of(
                                input("아침 복약", "2026-12-01T00:00:00Z"),
                                input("저녁 복약", "2026-12-01T10:00:00Z"))));
    }

    private String state() {
        return db.queryForObject(
                "select approval_status from action_items where id=?", String.class, candidate);
    }

    private int scheduleCount() {
        return db.queryForObject(
                "select count(*) from schedules where action_item_id=?", Integer.class, candidate);
    }

    private ResultActions call(User user, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + jwt.create(user)));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
