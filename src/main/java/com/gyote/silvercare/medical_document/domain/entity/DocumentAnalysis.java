package com.gyote.silvercare.medical_document.domain.entity;

import com.gyote.silvercare.medical_document.domain.DocumentAnalysisStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 의료문서 원본을 파싱·분석한 결과와 분석 처리 상태를 관리하는 엔티티입니다. */
@Entity
@Table(name = "document_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentAnalysis {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "parser_version", nullable = false, length = 50)
    private String parserVersion;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentAnalysisStatus status;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
