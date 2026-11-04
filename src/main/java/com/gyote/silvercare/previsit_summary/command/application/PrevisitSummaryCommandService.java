package com.gyote.silvercare.previsit_summary.command.application;

import com.gyote.silvercare.global.exception.*;
import com.gyote.silvercare.health_record.domain.HealthRecordAccessPolicy;
import com.gyote.silvercare.health_record.domain.repository.HealthRecordRepository;
import com.gyote.silvercare.previsit_summary.domain.PrevisitGenerationPort;
import com.gyote.silvercare.previsit_summary.domain.entity.*;
import com.gyote.silvercare.previsit_summary.domain.repository.*;
import com.gyote.silvercare.previsit_summary.error.PrevisitSummaryErrorCode;
import com.gyote.silvercare.previsit_summary.query.application.PrevisitSummaryQueryService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** 허용 기록의 스냅샷을 만든 뒤 외부 호출은 DB 트랜잭션 밖에서 한 번만 실행한다. */
@Service
public class PrevisitSummaryCommandService {
    private static final Logger log=LoggerFactory.getLogger(PrevisitSummaryCommandService.class);
    private final PrevisitSummaryRepository jobs;
    private final PrevisitSummarySourceRepository sources;
    private final HealthRecordRepository records;
    private final HealthRecordAccessPolicy access;
    private final PrevisitSummaryQueryService queries;
    private final UserRepository users;
    private final EntityManager em;
    private final PrevisitGenerationPort provider;
    private final TransactionTemplate tx;
    private final boolean enabled;

    public PrevisitSummaryCommandService(PrevisitSummaryRepository jobs, PrevisitSummarySourceRepository sources,
            HealthRecordRepository records, HealthRecordAccessPolicy access, PrevisitSummaryQueryService queries,
            UserRepository users, EntityManager em, PrevisitGenerationPort provider,
            PlatformTransactionManager manager, @Value("${silvercare.previsit.enabled:false}") boolean enabled) {
        this.jobs=jobs; this.sources=sources; this.records=records; this.access=access; this.queries=queries;
        this.users=users; this.em=em; this.provider=provider; this.tx=new TransactionTemplate(manager); this.enabled=enabled;
    }

    /** 본인 또는 ACTIVE 보호자가 정한 기간의 허용 기록만 대기열에 등록한다. */
    @Transactional
    public UUID create(User actor, UUID patientId, int days, String key) {
        UUID patient=access.requirePatient(actor,patientId);
        if(days<1 || days>30 || key==null || !key.matches("[A-Za-z0-9_-]{8,100}"))
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        em.find(User.class,actor.getId(),LockModeType.PESSIMISTIC_WRITE);
        var existing=jobs.findByRequestedByUserIdAndIdempotencyKey(actor.getId(),key);
        if(existing.isPresent()) {
            var row=existing.get();
            if(!row.getPatientId().equals(patient) || row.getWindowDays()!=days)
                throw new BusinessException(GlobalErrorCode.CONFLICT);
            queries.requireCurrent(actor,row); return row.getId();
        }
        var row=PrevisitSummary.create(patient,actor.getId(),key,days,Instant.now());
        var selected=records.findSummaryWindow(patient,row.getWindowStart(),row.getCreatedAt(),PageRequest.of(0,101));
        if(selected.size()>100 || selected.stream().mapToInt(r -> r.getContent().length()).sum()>60000)
            throw new BusinessException(PrevisitSummaryErrorCode.TOO_MANY_RECORDS);
        if(!selected.isEmpty() && (!enabled || !provider.available()))
            throw new BusinessException(PrevisitSummaryErrorCode.UNAVAILABLE);
        var saved=jobs.saveAndFlush(row);
        sources.saveAll(selected.stream().map(r -> PrevisitSummarySource.create(saved.getId(),r)).toList());
        if(selected.isEmpty()) saved.succeed("선택한 기간에 작성된 건강기록이 없어요.","","INSUFFICIENT");
        return saved.getId();
    }

    /** 대기 작업 하나를 선점한다. 중단 작업은 실패 처리하며 자동 외부 재호출하지 않는다. */
    public boolean processNext() {
        Ticket ticket=tx.execute(status -> claim());
        if(ticket==null) return false;
        PrevisitGenerationPort.Result result;
        try { result=provider.generate(ticket.input()); }
        catch(Exception failure) {
            // HTTP/공급자 경계의 원문·인증정보는 로그나 작업에 저장하지 않는다.
            log.warn("PREVISIT_PROVIDER_FAILED request_id={} error_type={}",ticket.id(),failure.getClass().getSimpleName());
            result=new PrevisitGenerationPort.Result(ticket.id(),null,List.of(),null,true,"AI_UNAVAILABLE",true);
        }
        var response=result;
        tx.executeWithoutResult(status -> finish(ticket,response));
        return true;
    }

    private Ticket claim() {
        for(var expired:jobs.findExpired(Instant.now().minusSeconds(600),PageRequest.of(0,20))) {
            var row=jobs.findLocked(expired.getId()).orElseThrow();
            if("RUNNING".equals(row.getJobStatus()) && row.getStartedAt().isBefore(Instant.now().minusSeconds(600)))
                row.fail("AI_TIMEOUT",true);
        }
        var pending=jobs.findQueued(PageRequest.of(0,1));
        if(pending.isEmpty()) return null;
        var row=pending.get(0);
        try {
            var selected=queries.requireCurrent(users.findById(row.getRequestedByUserId()).orElseThrow(),row);
            row.start();
            return new Ticket(row.getId(),row.getExecutionId(),new PrevisitGenerationPort.Input(row.getId(),row.getWindowDays(),
                    selected.stream().map(s -> new PrevisitGenerationPort.Source(s.getHealthRecordId(),s.getRecordedAt(),s.getContent())).toList()));
        } catch(BusinessException denied) { row.fail("SOURCE_UNAVAILABLE",false); return null; }
    }

    private void finish(Ticket ticket, PrevisitGenerationPort.Result result) {
        var row=jobs.findLocked(ticket.id()).orElseThrow();
        if(!"RUNNING".equals(row.getJobStatus()) || !ticket.execution().equals(row.getExecutionId())) return;
        try { queries.requireCurrent(users.findById(row.getRequestedByUserId()).orElseThrow(),row); }
        catch(BusinessException denied) { row.fail("SOURCE_UNAVAILABLE",false); return; }
        if(result==null || !row.getId().equals(result.requestId())) { row.fail("AI_RESPONSE_INVALID",false); return; }
        if(result.failed()) {
            var safe=Set.of("AI_TIMEOUT","AI_RATE_LIMIT","AI_API_KEY_MISSING","AI_RESPONSE_INVALID","AI_UNAVAILABLE","AI_AUTH_FAILED",
                    "AI_OUTPUT_LIMIT","AI_CONTENT_FILTERED");
            row.fail(result.errorCode()!=null && safe.contains(result.errorCode())?result.errorCode():"AI_UNAVAILABLE",result.retryable()); return;
        }
        var allowed=ticket.input().records().stream().map(PrevisitGenerationPort.Source::healthRecordId).collect(Collectors.toSet());
        var used=result.usedHealthRecordIds();
        if(result.summary()==null || result.summary().isBlank() || result.summary().length()>12000
                || used==null || used.isEmpty() || !allowed.containsAll(used) || new HashSet<>(used).size()!=used.size()
                || !"COMPLETE".equals(result.resultStatus())) { row.fail("AI_RESPONSE_INVALID",false); return; }
        row.succeed(result.summary(),used.stream().map(UUID::toString).collect(Collectors.joining(",")),"COMPLETE");
        log.info("PREVISIT_SUCCEEDED request_id={}",row.getId());
    }
    private record Ticket(UUID id,UUID execution,PrevisitGenerationPort.Input input) {}
}
