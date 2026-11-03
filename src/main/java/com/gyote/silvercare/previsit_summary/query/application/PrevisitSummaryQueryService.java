package com.gyote.silvercare.previsit_summary.query.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.health_record.domain.HealthRecordAccessPolicy;
import com.gyote.silvercare.health_record.domain.repository.HealthRecordRepository;
import com.gyote.silvercare.previsit_summary.domain.entity.PrevisitSummary;
import com.gyote.silvercare.previsit_summary.domain.entity.PrevisitSummarySource;
import com.gyote.silvercare.previsit_summary.domain.repository.*;
import com.gyote.silvercare.previsit_summary.error.PrevisitSummaryErrorCode;
import com.gyote.silvercare.previsit_summary.query.model.PrevisitSummaryView;
import com.gyote.silvercare.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/** 매 조회마다 현재 연결 권한과 원본 기록 버전을 검증한다. */
@Service
@Transactional(readOnly = true)
public class PrevisitSummaryQueryService {
    private final PrevisitSummaryRepository jobs;
    private final PrevisitSummarySourceRepository sources;
    private final HealthRecordRepository records;
    private final HealthRecordAccessPolicy access;
    public PrevisitSummaryQueryService(PrevisitSummaryRepository jobs,
            PrevisitSummarySourceRepository sources, HealthRecordRepository records,
            HealthRecordAccessPolicy access) {
        this.jobs=jobs; this.sources=sources; this.records=records; this.access=access;
    }

    /** 요청 식별자로 상태만 조회하며 본문은 결과 API에서 반환한다. */
    public PrevisitSummaryView.Operation operation(User actor, UUID request) {
        var row=jobs.findById(request).orElseThrow(() -> new BusinessException(PrevisitSummaryErrorCode.NOT_FOUND));
        requireCurrent(actor,row);
        return new PrevisitSummaryView.Operation(row.getId(),"PREVISIT_SUMMARY",row.getJobStatus(),
                row.getResultStatus(),row.getSummaryId(),row.isRetryable(),row.getErrorCode());
    }

    /** 현재 유효한 근거와 연결된 결과만 반환한다. */
    public PrevisitSummaryView.Detail detail(User actor, UUID id) {
        var row=jobs.findBySummaryId(id).orElseThrow(() -> new BusinessException(PrevisitSummaryErrorCode.NOT_FOUND));
        var allowed=requireCurrent(actor,row);
        var used=usedIds(row);
        return new PrevisitSummaryView.Detail(row.getSummaryId(),row.getSummary(),used,
                row.getWindowDays(),row.getCompletedAt(),allowed.stream()
                .filter(s -> used.contains(s.getHealthRecordId()))
                .map(s -> new PrevisitSummaryView.Source(s.getHealthRecordId(),s.getRecordedAt(),s.getContent())).toList());
    }

    /** 작업 실행 전후에도 같은 검증을 재사용해 해제·탈퇴·수정·삭제를 반영한다. */
    @Transactional(noRollbackFor = BusinessException.class)
    public List<PrevisitSummarySource> requireCurrent(User actor, PrevisitSummary row) {
        access.requirePatient(actor,row.getPatientId());
        var selected=sources.findByRequestIdOrderByRecordedAtAscHealthRecordIdAsc(row.getId());
        var current=records.findAllById(selected.stream().map(PrevisitSummarySource::getHealthRecordId).toList())
                .stream().collect(Collectors.toMap(r -> r.getId(),r -> r));
        for(var snapshot:selected) {
            var record=current.get(snapshot.getHealthRecordId());
            if(record==null || record.getDeletedAt()!=null || !record.getPatientId().equals(row.getPatientId())
                    || record.getVersion()!=snapshot.getRecordVersion()
                    || !record.getContent().equals(snapshot.getContent()))
                throw new BusinessException(PrevisitSummaryErrorCode.SOURCE_CHANGED);
        }
        return selected;
    }

    private List<UUID> usedIds(PrevisitSummary row) {
        return row.getUsedRecordIds()==null || row.getUsedRecordIds().isBlank() ? List.of()
                : Arrays.stream(row.getUsedRecordIds().split(",")).map(UUID::fromString).toList();
    }
}
