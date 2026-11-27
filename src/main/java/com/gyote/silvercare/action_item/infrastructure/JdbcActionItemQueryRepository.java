package com.gyote.silvercare.action_item.infrastructure;

import com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus;
import com.gyote.silvercare.action_item.domain.ActionItemType;
import com.gyote.silvercare.action_item.domain.repository.ActionItemQueryRepository;
import com.gyote.silvercare.action_item.query.model.ActionItemRow;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 제한된 후보 목록을 단일 조회로 읽고 다른 문서의 근거 연결을 차단한다. */
@Repository
public class JdbcActionItemQueryRepository implements ActionItemQueryRepository {
    private static final String SELECT =
            """
SELECT a.*, v.patient_id, d.file_name AS document_name,
       d.status AS document_status, d.deleted_at AS document_deleted_at,
       ex.id AS verified_source_id, ex.source_text, dp.page_no
FROM action_items a
JOIN documents d ON d.id = a.document_id AND d.visit_id = a.visit_id
JOIN visits v ON v.id = a.visit_id AND v.patient_id = d.patient_id
LEFT JOIN extracted_items ex ON ex.id = a.source_item_id
    AND EXISTS (SELECT 1 FROM document_analyses da
                WHERE da.id = ex.analysis_id AND da.document_id = d.id
                  AND da.status = 'SUCCEEDED')
    AND (ex.page_id IS NULL OR EXISTS (
        SELECT 1 FROM document_pages p WHERE p.id = ex.page_id AND p.document_id = d.id))
LEFT JOIN document_pages dp ON dp.id = ex.page_id AND dp.document_id = d.id
WHERE v.deleted_at IS NULL
""";
    private static final RowMapper<ActionItemRow> MAPPER = JdbcActionItemQueryRepository::map;
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcActionItemQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 개인·상태·커서 조건을 값 바인딩해 최대 limit건의 후보를 조회한다. */
    @Override
    public List<ActionItemRow> findPage(
            UUID patientId,
            ActionItemApprovalStatus status,
            Instant beforeCreatedAt,
            UUID beforeId,
            int limit) {
        StringBuilder sql = new StringBuilder(SELECT).append(" AND v.patient_id = :patientId");
        MapSqlParameterSource parameters =
                new MapSqlParameterSource("patientId", patientId).addValue("limit", limit);
        if (status != null) {
            sql.append(" AND a.approval_status = :status");
            parameters.addValue("status", status.name());
        }
        if (beforeCreatedAt != null) {
            sql.append(
                    " AND (a.created_at < :before OR (a.created_at = :before AND a.id <"
                        + " :beforeId))");
            parameters
                    .addValue("before", Timestamp.from(beforeCreatedAt))
                    .addValue("beforeId", beforeId);
        }
        sql.append(" ORDER BY a.created_at DESC, a.id DESC LIMIT :limit");
        return jdbc.query(sql.toString(), parameters, MAPPER);
    }

    /** 삭제 상태를 포함한 후보의 출처 메타데이터를 조합해 반환한다. */
    @Override
    public Optional<ActionItemRow> findById(UUID actionItemId) {
        return jdbc
                .query(
                        SELECT + " AND a.id = :id",
                        new MapSqlParameterSource("id", actionItemId),
                        MAPPER)
                .stream()
                .findFirst();
    }

    private static ActionItemRow map(ResultSet row, int index) throws SQLException {
        boolean deleted =
                "DELETED".equals(row.getString("document_status"))
                        || row.getTimestamp("document_deleted_at") != null;
        UUID sourceId = deleted ? null : row.getObject("verified_source_id", UUID.class);
        ActionItemRow.Source source =
                sourceId == null
                        ? null
                        : new ActionItemRow.Source(
                                row.getObject("page_no", Integer.class),
                                row.getString("source_text"));
        return new ActionItemRow(
                row.getObject("id", UUID.class),
                row.getObject("patient_id", UUID.class),
                row.getObject("visit_id", UUID.class),
                row.getObject("document_id", UUID.class),
                deleted ? null : row.getString("document_name"),
                ActionItemType.valueOf(row.getString("type")),
                row.getString("title"),
                row.getString("description"),
                instant(row, "due_at"),
                ActionItemApprovalStatus.valueOf(row.getString("approval_status")),
                row.getString("cancel_reason"),
                deleted,
                sourceId,
                row.getBigDecimal("confidence"),
                source,
                row.getObject("approved_by_user_id", UUID.class),
                instant(row, "approved_at"),
                instant(row, "created_at"));
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp timestamp = row.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
