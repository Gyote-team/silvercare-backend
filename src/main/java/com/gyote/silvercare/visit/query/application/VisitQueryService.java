package com.gyote.silvercare.visit.query.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.visit.domain.VisitAccessPolicy;
import com.gyote.silvercare.visit.domain.VisitStatus;
import com.gyote.silvercare.visit.domain.entity.Visit;
import com.gyote.silvercare.visit.domain.repository.VisitRepository;
import com.gyote.silvercare.visit.error.VisitErrorCode;
import com.gyote.silvercare.visit.query.model.VisitPageView;
import com.gyote.silvercare.visit.query.model.VisitView;

import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 권한 및 필터에 묶인 방문 커서 목록과 상세 참조를 반환한다. */
@Service
@Transactional(readOnly = true)
public class VisitQueryService {
    private final VisitRepository visits;
    private final VisitAccessPolicy access;
    private final JdbcTemplate jdbc;

    public VisitQueryService(VisitRepository visits, VisitAccessPolicy access, JdbcTemplate jdbc) {
        this.visits = visits;
        this.access = access;
        this.jdbc = jdbc;
    }

    /** 방문일 최신순 목록을 조회한다. 취소 방문은 기본 제외한다. */
    public VisitPageView list(
            User actor,
            UUID patientId,
            String cursor,
            Integer requestedSize,
            boolean includeCanceled) {
        UUID target = access.requirePatient(actor, patientId);
        int size = requestedSize == null ? 20 : requestedSize;
        if (size < 1 || size > 100) throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        LocalDate before = null;
        UUID beforeId = null;
        if (cursor != null) {
            try {
                String[] parts =
                        new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
                                .split("\\|", -1);
                if (parts.length != 4
                        || !target.equals(UUID.fromString(parts[0]))
                        || !String.valueOf(includeCanceled).equals(parts[1]))
                    throw new IllegalArgumentException();
                before = LocalDate.parse(parts[2]);
                beforeId = UUID.fromString(parts[3]);
            } catch (RuntimeException error) {
                throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
            }
        }
        List<Visit> rows =
                before == null
                        ? visits.findFirstPage(
                                target,
                                includeCanceled,
                                VisitStatus.CANCELED,
                                PageRequest.of(0, size + 1))
                        : visits.findPage(
                                target,
                                includeCanceled,
                                VisitStatus.CANCELED,
                                before,
                                beforeId,
                                PageRequest.of(0, size + 1));
        boolean more = rows.size() > size;
        List<Visit> shown = rows.subList(0, Math.min(size, rows.size()));
        String next = null;
        if (more) {
            Visit last = shown.get(shown.size() - 1);
            next =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(
                                    (target
                                                    + "|"
                                                    + includeCanceled
                                                    + "|"
                                                    + last.getVisitedOn()
                                                    + "|"
                                                    + last.getId())
                                            .getBytes(StandardCharsets.UTF_8));
        }
        return new VisitPageView(toViews(shown), next, more);
    }

    /** 취소된 방문도 당사자가 상세 및 하위 자료 참조를 확인할 수 있다. */
    public VisitView detail(User actor, UUID id) {
        Visit visit =
                visits.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new BusinessException(VisitErrorCode.NOT_FOUND));
        access.requirePatient(actor, visit.getPatientId());
        return toViews(List.of(visit)).get(0);
    }

    private List<VisitView> toViews(List<Visit> rows) {
        if (rows.isEmpty()) return List.of();
        List<UUID> ids = rows.stream().map(Visit::getId).toList();
        String parameters = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        Map<UUID, List<UUID>> documents =
                references(
                        "select visit_id,id from documents where status <> 'DELETED' and visit_id"
                            + " in ("
                                + parameters
                                + ") order by id",
                        ids);
        Map<UUID, List<UUID>> records =
                references(
                        "select visit_id,id from health_records where deleted_at is null and"
                            + " visit_id in ("
                                + parameters
                                + ") order by id",
                        ids);
        return rows.stream()
                .map(
                        v ->
                                new VisitView(
                                        v.getId(),
                                        v.getPatientId(),
                                        v.getVisitedOn(),
                                        v.getHospitalName(),
                                        v.getDepartment(),
                                        v.getMemo(),
                                        v.getStatus(),
                                        documents.getOrDefault(v.getId(), List.of()),
                                        records.getOrDefault(v.getId(), List.of()),
                                        v.getCreatedAt(),
                                        v.getUpdatedAt()))
                .toList();
    }

    private Map<UUID, List<UUID>> references(String sql, List<UUID> ids) {
        Map<UUID, List<UUID>> result = new HashMap<>();
        jdbc.query(
                sql,
                rs -> {
                    UUID visitId = UUID.fromString(rs.getString(1));
                    UUID id = UUID.fromString(rs.getString(2));
                    result.computeIfAbsent(visitId, key -> new ArrayList<>()).add(id);
                },
                ids.toArray());
        return result;
    }
}
