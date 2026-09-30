package com.gyote.silvercare.medical_document.domain.entity;

import com.gyote.silvercare.global.status.ResultStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** AI가 의료문서를 환자가 이해하기 쉬운 설명으로 변환한 결과를 관리하는 엔티티입니다. */
@Entity
@Table(name = "ai_explanations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiExplanation {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "ai_run_id")
    private UUID aiRunId;

    @Column(nullable = false)
    private Integer version;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", length = 20)
    private ResultStatus resultStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

}
