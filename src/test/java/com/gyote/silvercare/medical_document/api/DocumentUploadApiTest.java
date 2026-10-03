package com.gyote.silvercare.medical_document.api;

import com.gyote.silvercare.patient.domain.repository.PatientRepository;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 업로드 API가 HTTP로 201, 같은 키 재요청 200, 비로그인 401을 돌려주는지 확인하는 통합 테스트입니다. */
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
        loginCookies = demoLoginCookies();
        visitId = UUID.randomUUID();
        jdbc.update("INSERT INTO visits (id, patient_id, visited_on) VALUES (?, ?, ?)",
                visitId, demoPatientId(), LocalDate.of(2026, 9, 20));
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM document_analyses WHERE document_id IN "
                + "(SELECT id FROM documents WHERE visit_id = ?)", visitId);
        jdbc.update("DELETE FROM documents WHERE visit_id = ?", visitId);
        jdbc.update("DELETE FROM visits WHERE id = ?", visitId);
    }

    @Test
    void uploadIsCreatedThenOkForSameKeyAndUnauthorizedWithoutLogin() throws Exception {
        String key = "api-" + UUID.randomUUID();

        String created = mvc.perform(uploadRequest(key).cookie(loginCookies))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentStatus").value("UPLOADED"))
                .andExpect(jsonPath("$.latestAiJobStatus").value("QUEUED"))
                .andReturn().getResponse().getContentAsString();
        String documentId = JsonPath.read(created, "$.documentId");

        mvc.perform(uploadRequest(key).cookie(loginCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId));

        mvc.perform(uploadRequest(key))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder uploadRequest(String key) throws IOException {
        return multipart("/api/visits/{visitId}/documents", visitId)
                .file(new MockMultipartFile("file", "검사결과.pdf", "application/pdf", pdf()))
                .header("Idempotency-Key", key);
    }

    private Cookie[] demoLoginCookies() throws Exception {
        return mvc.perform(post("/api/demo/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"PATIENT\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookies();
    }

    private UUID demoPatientId() {
        UUID userId = users.findByKakaoId("demo-patient").orElseThrow().getId();
        return patients.findByUserId(userId).orElseThrow().getId();
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
