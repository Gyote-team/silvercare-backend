package com.gyote.silvercare.ai_result.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@DataJpaTest
class ResultDocumentEvidenceRepositoryTest {
    @Autowired ResultDocumentEvidenceRepository evidence;
    @Autowired JdbcTemplate jdbc;

    private final UUID patientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS document_pages (
                    id UUID PRIMARY KEY, page_no INTEGER NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS extracted_items (
                    id UUID PRIMARY KEY, analysis_id UUID NOT NULL, page_id UUID,
                    normalized_name VARCHAR(200), raw_value VARCHAR(500), unit VARCHAR(50),
                    reference_range VARCHAR(100), source_text VARCHAR(1000) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
    }

    @Test
    void dividesFortyItemsEvenlyAcrossTwoDocuments() {
        UUID older = document(Instant.parse("2026-10-01T00:00:00Z"));
        UUID newer = document(Instant.parse("2026-10-02T00:00:00Z"));
        items(analysis(older, "SUCCEEDED", Instant.parse("2026-10-01T01:00:00Z")), 40);
        items(analysis(newer, "SUCCEEDED", Instant.parse("2026-10-02T01:00:00Z")), 40);

        var rows = evidence.findEvidence(patientId, List.of(older, newer));

        assertThat(rows).hasSize(40);
        assertThat(count(rows, older)).isEqualTo(20);
        assertThat(count(rows, newer)).isEqualTo(20);
    }

    @Test
    void givesUnusedPlacesToTheOtherDocument() {
        UUID smaller = document(Instant.parse("2026-10-01T00:00:00Z"));
        UUID larger = document(Instant.parse("2026-10-02T00:00:00Z"));
        items(analysis(smaller, "SUCCEEDED", Instant.parse("2026-10-01T01:00:00Z")), 5);
        items(analysis(larger, "SUCCEEDED", Instant.parse("2026-10-02T01:00:00Z")), 40);

        var rows = evidence.findEvidence(patientId, List.of(smaller, larger));

        assertThat(rows).hasSize(40);
        assertThat(count(rows, smaller)).isEqualTo(5);
        assertThat(count(rows, larger)).isEqualTo(35);
    }

    @Test
    void usesLatestSuccessfulAnalysisAfterRetryFailure() {
        UUID reanalyzed = document(Instant.parse("2026-10-01T00:00:00Z"));
        UUID failed = document(Instant.parse("2026-10-02T00:00:00Z"));
        items(analysis(reanalyzed, "SUCCEEDED", Instant.parse("2026-10-01T01:00:00Z")), 7);
        items(analysis(reanalyzed, "SUCCEEDED", Instant.parse("2026-10-01T02:00:00Z")), 3);
        items(analysis(failed, "SUCCEEDED", Instant.parse("2026-10-02T01:00:00Z")), 4);
        analysis(failed, "FAILED", Instant.parse("2026-10-02T02:00:00Z"));

        var rows = evidence.findEvidence(patientId, List.of(reanalyzed, failed));

        assertThat(rows).hasSize(7);
        assertThat(count(rows, reanalyzed)).isEqualTo(3);
        assertThat(count(rows, failed)).isEqualTo(4);
    }

    @Test
    void returnsNoEvidenceWhenDocumentHasNeverSucceeded() {
        UUID failed = document(Instant.parse("2026-10-02T00:00:00Z"));
        analysis(failed, "FAILED", Instant.parse("2026-10-02T02:00:00Z"));

        assertThat(evidence.findEvidence(patientId, List.of(failed))).isEmpty();
    }

    private long count(List<ResultDocumentEvidenceRepository.EvidenceRow> rows, UUID documentId) {
        return rows.stream().filter(row -> documentId.toString().equals(row.getDocumentId())).count();
    }

    private UUID document(Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO documents
                    (id, patient_id, visit_id, uploader_user_id, file_name, storage_key, mime_type,
                     file_size_bytes, document_type, status, status_changed_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, patientId, UUID.randomUUID(), UUID.randomUUID(), "검사.pdf",
                "documents/" + id, "application/pdf", 100L, "UNKNOWN", "UPLOADED",
                Timestamp.from(createdAt), Timestamp.from(createdAt), Timestamp.from(createdAt));
        return id;
    }

    private UUID analysis(UUID documentId, String status, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO document_analyses
                    (id, document_id, parser_version, status, retryable, retry_count, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, documentId, "test", status, false, 0, Timestamp.from(createdAt));
        return id;
    }

    private void items(UUID analysisId, int count) {
        for (int index = 0; index < count; index++) {
            jdbc.update("""
                    INSERT INTO extracted_items (id, analysis_id, source_text, created_at)
                    VALUES (?, ?, ?, ?)
                    """, UUID.randomUUID(), analysisId, "검사 항목 " + index,
                    Timestamp.from(Instant.parse("2026-10-03T00:00:00Z").plusSeconds(index)));
        }
    }
}
