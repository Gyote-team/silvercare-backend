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

/** AI 설명 항목이 참조한 원문 근거와 인용 정보를 관리하는 엔티티입니다. */
@Entity
@Table(name = "explanation_citations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExplanationCitation {

    @Id
    private UUID id;

    @Column(name = "explanation_id", nullable = false)
    private UUID explanationId;

    @Column(name = "claim_id")
    private UUID claimId;

    @Column(name = "explanation_item_id")
    private UUID explanationItemId;

    @Column(name = "page_id")
    private UUID pageId;

    @Column(name = "chunk_id")
    private UUID chunkId;

    @Column(name = "quoted_text")
    private String quotedText;

    @Column(name = "anchor_id", length = 200)
    private String anchorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
