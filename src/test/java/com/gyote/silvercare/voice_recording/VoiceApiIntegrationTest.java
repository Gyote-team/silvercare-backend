package com.gyote.silvercare.voice_recording;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyote.silvercare.care_relation.command.application.CareRelationCommandService;
import com.gyote.silvercare.global.auth.application.JwtService;
import com.gyote.silvercare.health_record.domain.repository.HealthRecordRepository;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.*;
import com.gyote.silvercare.visit.domain.VisitStatus;
import com.gyote.silvercare.visit.domain.entity.Visit;
import com.gyote.silvercare.visit.domain.repository.VisitRepository;
import com.gyote.silvercare.voice_recording.domain.repository.VoiceTranscriptionRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** 실제 인증·권한·초안 확정·중복 및 취소 경쟁의 HTTP 계약을 검증한다. */
@SpringBootTest(properties = "silvercare.voice.enabled=true")
@AutoConfigureMockMvc
@Transactional
class VoiceApiIntegrationTest {
    @TempDir static Path storage;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) {
        registry.add("silvercare.storage.local-root", () -> storage.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountService accounts;
    @Autowired JwtService jwt;
    @Autowired CareRelationCommandService cares;
    @Autowired PatientRepository patients;
    @Autowired VisitRepository visits;
    @Autowired VoiceTranscriptionRepository jobs;
    @Autowired HealthRecordRepository records;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    User personal, caregiver;
    UUID patient;
    String token, caregiverToken;

    @BeforeEach
    void setup() {
        String id = UUID.randomUUID().toString();
        personal =
                accounts.chooseRole(
                        accounts.loginOrRegister("voice-p-" + id, "개인").getKakaoId(),
                        UserRole.PATIENT);
        caregiver =
                accounts.chooseRole(
                        accounts.loginOrRegister("voice-c-" + id, "보호자").getKakaoId(),
                        UserRole.CAREGIVER);
        patient = patients.findByUserId(personal.getId()).orElseThrow().getId();
        token = jwt.create(personal);
        caregiverToken = jwt.create(caregiver);
    }

    byte[] wav() {
        byte[] b = new byte[48];
        System.arraycopy("RIFF".getBytes(), 0, b, 0, 4);
        System.arraycopy("WAVE".getBytes(), 0, b, 8, 4);
        return b;
    }

    MockMultipartFile audio(byte[] bytes) {
        return new MockMultipartFile("audio", "../../untrusted.exe", "text/plain", bytes);
    }

    UUID upload(String key) throws Exception {
        return UUID.fromString(
                json.readTree(
                                mvc.perform(
                                                multipart("/api/health-records/voice")
                                                        .file(audio(wav()))
                                                        .param("consent", "true")
                                                        .header("Idempotency-Key", key)
                                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isAccepted())
                                        .andExpect(header().exists("Location"))
                                        .andExpect(jsonPath("$.storageKey").doesNotExist())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .get("transcriptionId")
                        .asText());
    }

    void draft(UUID id) {
        em.flush();
        jdbc.update(
                "update voice_transcriptions set"
                        + " status='DRAFT_READY',draft_text=?,version=version+1 where id=?",
                "인식된 초안",
                id);
        em.clear();
    }

    UUID visit() {
        return visits.saveAndFlush(
                        Visit.create(patient, LocalDate.now(), "병원", "내과", null, VisitStatus.DONE))
                .getId();
    }

    String confirmBody(String body, long version) throws Exception {
        return json.writeValueAsString(Map.of("body", body, "version", version));
    }

    @Test
    void uploadOnlyCreatesDraftAndReplayIsIdempotent() throws Exception {
        long count = records.count();
        String key = UUID.randomUUID().toString();
        UUID id = upload(key);
        assertThat(upload(key)).isEqualTo(id);
        assertThat(records.count()).isEqualTo(count);
        var row = jobs.findById(id).orElseThrow();
        assertThat(row.getStorageKey()).endsWith(".wav");
        assertThat(Files.readAllBytes(storage.resolve(row.getStorageKey()))).isEqualTo(wav());
        mvc.perform(get("/api/transcriptions/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"));
        byte[] other = wav();
        other[47] = 1;
        mvc.perform(
                        multipart("/api/health-records/voice")
                                .file(audio(other))
                                .param("consent", "true")
                                .header("Idempotency-Key", key)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VOICE_003"));
    }

    @Test
    void confirmSavesEditedTextOnceAsSpeechInput() throws Exception {
        UUID id = upload(UUID.randomUUID().toString());
        long count = records.count();
        draft(id);
        String body = confirmBody("확인하고 고친 내용", 1);
        for (int i = 0; i < 2; i++)
            mvc.perform(
                            post("/api/transcriptions/" + id + "/confirm")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.recordId").isNotEmpty());
        assertThat(records.count()).isEqualTo(count + 1);
        var row = jobs.findById(id).orElseThrow();
        var record = records.findById(row.getRecordId()).orElseThrow();
        assertThat(record.getContent()).isEqualTo("확인하고 고친 내용");
        assertThat(record.getInputType()).isEqualTo("STT");
        mvc.perform(
                        post("/api/transcriptions/" + id + "/confirm")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(confirmBody("다른 내용", 1)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/transcriptions/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void unreadyCanceledFailedAndStaleDraftCannotConfirm() throws Exception {
        UUID id = upload(UUID.randomUUID().toString());
        long count = records.count();
        mvc.perform(
                        post("/api/transcriptions/" + id + "/confirm")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(confirmBody("내용", 0)))
                .andExpect(status().isConflict());
        draft(id);
        mvc.perform(
                        post("/api/transcriptions/" + id + "/confirm")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(confirmBody("내용", 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GLOBAL_004"));
        for (int i = 0; i < 2; i++)
            mvc.perform(
                            delete("/api/transcriptions/" + id)
                                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        mvc.perform(
                        post("/api/transcriptions/" + id + "/confirm")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(confirmBody("내용", 2)))
                .andExpect(status().isConflict());
        assertThat(records.count()).isEqualTo(count);
    }

    @Test
    void requestedAndRevokedCaregiverCannotReadOrConfirm() throws Exception {
        UUID id = upload(UUID.randomUUID().toString());
        draft(id);
        var relation = cares.request(caregiver, accounts.patientInviteCode(personal));
        mvc.perform(
                        get("/api/transcriptions/" + id)
                                .header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isForbidden());
        cares.accept(personal, relation.getId());
        mvc.perform(
                        get("/api/transcriptions?patientId=" + patient)
                                .header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transcriptionId").value(id.toString()));
        mvc.perform(
                        multipart("/api/health-records/voice")
                                .file(audio(wav()))
                                .param("consent", "true")
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isBadRequest());
        cares.revoke(personal, relation.getId());
        for (String path :
                new String[] {
                    "/api/transcriptions/" + id, "/api/transcriptions?patientId=" + patient
                })
            mvc.perform(get(path).header("Authorization", "Bearer " + caregiverToken))
                    .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/transcriptions/" + id + "/confirm")
                                .header("Authorization", "Bearer " + caregiverToken)
                                .contentType("application/json")
                                .content(confirmBody("내용", 1)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        delete("/api/transcriptions/" + id)
                                .header("Authorization", "Bearer " + caregiverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void visitRecordingRequiresConsentAndPreservesVisitOnConfirm() throws Exception {
        UUID visit = visit();
        String url = "/api/visits/" + visit + "/recordings";
        mvc.perform(
                        multipart(url)
                                .file(audio(wav()))
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        UUID id =
                UUID.fromString(
                        json.readTree(
                                        mvc.perform(
                                                        multipart(url)
                                                                .file(audio(wav()))
                                                                .param("consent", "true")
                                                                .header(
                                                                        "Idempotency-Key",
                                                                        UUID.randomUUID()
                                                                                .toString())
                                                                .header(
                                                                        "Authorization",
                                                                        "Bearer " + token))
                                                .andExpect(status().isAccepted())
                                                .andExpect(
                                                        jsonPath("$.purpose").value("CONSULTATION"))
                                                .andReturn()
                                                .getResponse()
                                                .getContentAsString())
                                .get("transcriptionId")
                                .asText());
        draft(id);
        mvc.perform(
                        post("/api/transcriptions/" + id + "/confirm")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(confirmBody("확인한 진료 내용", 1)))
                .andExpect(status().isOk());
        assertThat(
                        records.findById(jobs.findById(id).orElseThrow().getRecordId())
                                .orElseThrow()
                                .getVisitId())
                .isEqualTo(visit);
        mvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void canceledVisitCannotUploadOrConfirmExistingDraft() throws Exception {
        UUID visit = visit();
        UUID id =
                UUID.fromString(
                        json.readTree(
                                        mvc.perform(
                                                        multipart("/api/health-records/voice")
                                                                .file(audio(wav()))
                                                                .param("visitId", visit.toString())
                                                                .param("consent", "true")
                                                                .header(
                                                                        "Idempotency-Key",
                                                                        UUID.randomUUID()
                                                                                .toString())
                                                                .header(
                                                                        "Authorization",
                                                                        "Bearer " + token))
                                                .andReturn()
                                                .getResponse()
                                                .getContentAsString())
                                .get("transcriptionId")
                                .asText());
        draft(id);
        visits.findById(visit).orElseThrow().cancel();
        em.flush();
        mvc.perform(
                        multipart("/api/visits/" + visit + "/recordings")
                                .file(audio(wav()))
                                .param("consent", "true")
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/transcriptions/" + id + "/confirm")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(confirmBody("내용", 1)))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        get("/api/visits/" + visit + "/recordings")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void invalidKeyFileSizeAndBodyAreRejected() throws Exception {
        mvc.perform(
                        multipart("/api/health-records/voice")
                                .file(audio(wav()))
                                .param("consent", "true")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        for (byte[] bytes : new byte[][] {new byte[0], "not an audio recording file".getBytes()})
            mvc.perform(
                            multipart("/api/health-records/voice")
                                    .file(audio(bytes))
                                    .param("consent", "true")
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnsupportedMediaType());
        mvc.perform(
                        multipart("/api/health-records/voice")
                                .file(audio(new byte[20 * 1024 * 1024 + 1]))
                                .param("consent", "true")
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isPayloadTooLarge());
        UUID id = upload(UUID.randomUUID().toString());
        draft(id);
        for (String body : new String[] {" ", "a".repeat(2001)})
            mvc.perform(
                            post("/api/transcriptions/" + id + "/confirm")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType("application/json")
                                    .content(confirmBody(body, 1)))
                    .andExpect(status().isBadRequest());
        mvc.perform(get("/api/transcriptions/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void failedJobCanRetryAndLateResultCannotReviveCanceledJob() throws Exception {
        UUID id = upload(UUID.randomUUID().toString());
        em.flush();
        UUID lease = UUID.randomUUID();
        jdbc.update(
                "update voice_transcriptions set status='PROCESSING',lease_token=? where id=?",
                lease,
                id);
        em.clear();
        mvc.perform(delete("/api/transcriptions/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        em.flush();
        assertThat(
                        jdbc.update(
                                "update voice_transcriptions set"
                                        + " status='DRAFT_READY',draft_text='late' where id=? and"
                                        + " status='PROCESSING' and lease_token=?",
                                id,
                                lease))
                .isZero();
        UUID failed = upload(UUID.randomUUID().toString());
        em.flush();
        jdbc.update(
                "update voice_transcriptions set status='FAILED',error_code='STT_TIMEOUT' where"
                        + " id=?",
                failed);
        em.clear();
        mvc.perform(
                        post("/api/transcriptions/" + failed + "/confirm")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(confirmBody("내용", 0)))
                .andExpect(status().isConflict());
        mvc.perform(
                        post("/api/transcriptions/" + failed + "/retry")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.errorCode").isEmpty());
        mvc.perform(
                        post("/api/transcriptions/" + failed + "/retry")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }
}
