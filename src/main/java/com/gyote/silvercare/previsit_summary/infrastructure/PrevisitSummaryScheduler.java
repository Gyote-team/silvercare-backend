package com.gyote.silvercare.previsit_summary.infrastructure;

import com.gyote.silvercare.previsit_summary.command.application.PrevisitSummaryCommandService;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

/** 영속 대기 요청을 API 스레드와 분리하여 처리한다. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix="silvercare.previsit",name="processing-enabled",havingValue="true",matchIfMissing=true)
public class PrevisitSummaryScheduler {
    private static final Logger log=LoggerFactory.getLogger(PrevisitSummaryScheduler.class);
    private final PrevisitSummaryCommandService commands;
    public PrevisitSummaryScheduler(PrevisitSummaryCommandService commands){this.commands=commands;}
    /** DB 장애에서도 다음 주기에 대기열을 다시 확인한다. */
    @Scheduled(fixedDelayString="${silvercare.previsit.poll-ms:2000}")
    public void process() {
        try { commands.processNext(); }
        catch(RuntimeException failure) { log.warn("PREVISIT_WORKER_ERROR type={}",failure.getClass().getSimpleName()); }
    }
}
