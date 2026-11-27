package com.gyote.silvercare.action_item.domain.repository;

import com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus;
import com.gyote.silvercare.action_item.query.model.ActionItemRow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 할 일 후보와 소유 문서·원문 근거를 함께 읽는 저장소 계약이다. */
public interface ActionItemQueryRepository {
    /** 개인·상태 범위에 속하는 후보를 생성 시각과 ID 내림차순으로 제한 조회한다. */
    List<ActionItemRow> findPage(
            UUID patientId,
            ActionItemApprovalStatus status,
            Instant beforeCreatedAt,
            UUID beforeId,
            int limit);

    /** 유효한 소유 문서·방문 연결이 있는 후보 상세를 반환한다. */
    Optional<ActionItemRow> findById(UUID actionItemId);
}
