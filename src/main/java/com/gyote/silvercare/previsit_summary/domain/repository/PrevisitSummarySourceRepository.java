package com.gyote.silvercare.previsit_summary.domain.repository;

import com.gyote.silvercare.previsit_summary.domain.entity.PrevisitSummarySource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

/** 서버가 허용한 원본 기록 스냅샷만 조회한다. */
public interface PrevisitSummarySourceRepository extends JpaRepository<PrevisitSummarySource, UUID> {
    List<PrevisitSummarySource> findByRequestIdOrderByRecordedAtAscHealthRecordIdAsc(UUID requestId);
}
