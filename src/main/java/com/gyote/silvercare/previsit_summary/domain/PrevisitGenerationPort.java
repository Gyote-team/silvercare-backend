package com.gyote.silvercare.previsit_summary.domain;

import java.time.Instant;
import java.util.*;

/** 허용된 건강기록만 외부 요약기에 전달하는 경계. */
public interface PrevisitGenerationPort {
    boolean available();
    Result generate(Input input);
    record Source(UUID healthRecordId, Instant recordedAt, String content) {}
    record Input(UUID requestId, int windowDays, List<Source> records) {}
    record Result(UUID requestId, String summary, List<UUID> usedHealthRecordIds,
                  String resultStatus, boolean failed, String errorCode, boolean retryable) {}
}
