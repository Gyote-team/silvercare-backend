package com.gyote.silvercare.medical_document.domain.entity;

import com.gyote.silvercare.medical_document.domain.ExplanationSectionType;

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

/** AI 설명을 검사 결과·복약·주의사항 등의 주제별 섹션으로 나누어 관리하는 엔티티입니다. */
@Entity
@Table(name = "explanation_sections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExplanationSection {

    @Id
    private UUID id;

    @Column(name = "explanation_id", nullable = false)
    private UUID explanationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 30)
    private ExplanationSectionType sectionType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "section_order", nullable = false)
    private Integer sectionOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
