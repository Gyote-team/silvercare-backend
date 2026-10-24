package com.gyote.silvercare.medical_document.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** 설명 섹션 안에서 사용자에게 보여줄 개별 설명 항목과 값을 관리하는 엔티티입니다. */
@Entity
@Table(name = "explanation_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExplanationItem {

    @Id
    private UUID id;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Column(name = "extracted_item_id")
    private UUID extractedItemId;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "display_value", length = 500)
    private String displayValue;

    @Column(length = 50)
    private String unit;

    @Column(name = "has_source", nullable = false)
    private boolean hasSource;

    @Column(name = "item_order", nullable = false)
    private Integer itemOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
