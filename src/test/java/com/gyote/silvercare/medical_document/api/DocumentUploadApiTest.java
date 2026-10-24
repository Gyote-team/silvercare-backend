package com.gyote.silvercare.medical_document.api;

import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.command.application.UserAccountService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 업로드 API가 HTTP로 201, 같은 키 재요청 200, 비로그인 401과 오류 응답(400·403·404·415)을 돌려주는지 확인하는 통합 테스트입니다. */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentUploadApiTest {

    @TempDir
    static Path storageRoot;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private UserAccountService accounts;

    private final List<UUID> visitIds = new ArrayList<>();
    private Cookie[] loginCookies;
    private UUID visitId;

    /** 원본 파일이 프로젝트 폴더가 아니라 임시 폴더에 저장되게 합니다. */
    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("silvercare.storage.local-root", () -> storageRoot.toString());
    }

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS visits (
                    id UUID PRIMARY KEY,
                    patient_id UUID NOT NULL,
                    visited_on DATE NOT NULL
                )
                """);
        jdbc.execute("ALTER TABLE visits ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP");
        loginCookies = demoLoginCookies("PATIENT");
        visitId = insertVisit(demoPatientId());
    }

    @AfterEach
    void tearDown() {
        for (UUID id : visitIds) {
            jdbc.update("DELETE FROM document_analyses WHERE document_id IN "
                    + "(SELECT id FROM documents WHERE visit_id = ?)", id);
            jdbc.update("DELETE FROM documents WHERE visit_id = ?", id);
            jdbc.update("DELETE FROM visits WHERE id = ?", id);
        }
        visitIds.clear();
    }

    @Test
    void uploadIsCreatedThenOkForSameKeyAndUnauthorizedWithoutLogin() throws Exception {
        String key = "api-" + UUID.randomUUID();
        MockMultipartFile file = pdfFile();

        String created = mvc.perform(uploadRequest(key, file).cookie(loginCookies))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentStatus").value("UPLOADED"))
                .andExpect(jsonPath("$.latestAiJobStatus").value("QUEUED"))
                .andReturn().getResponse().getContentAsString();
        String documentId = JsonPath.read(created, "$.documentId");

        mvc.perform(uploadRequest(key, file).cookie(loginCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId));

        mvc.perform(uploadRequest(key, file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingIdempotencyKeyIsBadRequest() throws Exception {
        assertError(upload(visitId, pdfFile()).cookie(loginCookies), 400, "MEDICAL_DOCUMENT_013");
    }

    @Test
    void missingVisitIsNotFound() throws Exception {
        assertError(withKey(upload(UUID.randomUUID(), pdfFile())).cookie(loginCookies),
                404, "MEDICAL_DOCUMENT_012");
    }

    @Test
    void unconnectedCaregiverIsForbidden() throws Exception {
        UUID otherVisitId = insertVisit(newPatientId());

        assertError(withKey(upload(otherVisitId, pdfFile())).cookie(demoLoginCookies("CAREGIVER")),
                403, "MEDICAL_DOCUMENT_002");
    }

    @Test
    void unsupportedFileIsUnsupportedMediaType() throws Exception {
        MockMultipartFile text = new MockMultipartFile("file", "메모.txt", "text/plain", "hello".getBytes());

        assertError(withKey(upload(visitId, text)).cookie(loginCookies), 415, "MEDICAL_DOCUMENT_008");
    }

    @Test
    void pdfSentAsOctetStreamIsCreatedAsPdf() throws Exception {
        MockMultipartFile octetStream =
                new MockMultipartFile("file", "검사결과.pdf", "application/octet-stream", pdf());

        mvc.perform(withKey(upload(visitId, octetStream)).cookie(loginCookies))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mimeType").value("application/pdf"));
    }

    /** 요청이 주어진 상태 코드와 오류 code로 끝나는지 확인합니다. */
    private void assertError(MockHttpServletRequestBuilder request, int status, String code) throws Exception {
        mvc.perform(request)
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code));
    }

    private MockHttpServletRequestBuilder uploadRequest(String key, MockMultipartFile file) {
        return upload(visitId, file).header("Idempotency-Key", key);
    }

    /** Idempotency-Key 없이 파일만 담은 업로드 요청을 만듭니다. */
    private MockHttpServletRequestBuilder upload(UUID targetVisitId, MockMultipartFile file) {
        return multipart("/api/visits/{visitId}/documents", targetVisitId).file(file);
    }

    private MockHttpServletRequestBuilder withKey(MockHttpServletRequestBuilder request) {
        return request.header("Idempotency-Key", "api-" + UUID.randomUUID());
    }

    private MockMultipartFile pdfFile() throws IOException {
        return new MockMultipartFile("file", "검사결과.pdf", "application/pdf", pdf());
    }

    private Cookie[] demoLoginCookies(String role) throws Exception {
        return mvc.perform(post("/api/demo/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookies();
    }

    private UUID demoPatientId() {
        UUID userId = users.findByKakaoId("demo-patient").orElseThrow().getId();
        return patients.findByUserId(userId).orElseThrow().getId();
    }

    /** 어떤 보호자와도 연결되지 않은 새 환자를 만들어 patientId를 반환합니다. */
    private UUID newPatientId() {
        String kakaoId = accounts.loginOrRegister("kakao-" + UUID.randomUUID(), "새환자").getKakaoId();
        User patient = accounts.chooseRole(kakaoId, UserRole.PATIENT);
        return patients.findByUserId(patient.getId()).orElseThrow().getId();
    }

    /** 환자의 방문을 하나 넣고 id를 반환합니다. tearDown이 지우도록 기록해 둡니다. */
    private UUID insertVisit(UUID patientId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO visits (id, patient_id, visited_on) VALUES (?, ?, ?)",
                id, patientId, LocalDate.of(2026, 9, 20));
        visitIds.add(id);
        return id;
    }

    private static byte[] pdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
