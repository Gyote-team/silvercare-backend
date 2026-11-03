package com.gyote.silvercare.previsit_summary.domain.entity;

import com.gyote.silvercare.health_record.domain.entity.HealthRecord;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/** 허용한 기록의 버전과 본문을 보관하며 원본 변경 여부를 조회마다 검증한다. */
@Entity
@Table(name = "previsit_summary_sources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrevisitSummarySource {
    @Id private UUID id;
    @Column(nullable = false) private UUID requestId;
    @Column(nullable = false) private UUID healthRecordId;
    @Column(nullable = false) private long recordVersion;
    @Column(nullable = false) private Instant recordedAt;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;

    /** 선정된 기록만 AI 입력 스냅샷에 포함한다. */
    public static PrevisitSummarySource create(UUID request, HealthRecord record) {
        var row = new PrevisitSummarySource();
        row.id = UUID.randomUUID(); row.requestId = request; row.healthRecordId = record.getId();
        row.recordVersion = record.getVersion(); row.recordedAt = record.getRecordedAt();
        row.content = record.getContent(); return row;
    }
}
