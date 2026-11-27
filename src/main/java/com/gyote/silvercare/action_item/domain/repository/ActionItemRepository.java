package com.gyote.silvercare.action_item.domain.repository;

import com.gyote.silvercare.action_item.domain.entity.ActionItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ActionItemRepository extends JpaRepository<ActionItem, UUID> {

    /** 문서 잠금 순서를 정하기 위해 후보의 원본 문서 ID만 조회한다. */
    @Query("select a.documentId from ActionItem a where a.id=:id")
    java.util.Optional<UUID> findDocumentId(@Param("id") UUID id);

    /** 같은 후보의 승인·거절을 직렬화한다. 원본 문서를 먼저 잠가야 한다. */
    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ActionItem a where a.id=:id")
    java.util.Optional<ActionItem> findLockedById(@Param("id") UUID id);

    /** 문서에서 나온 PENDING 후보를 모두 CANCELED로 바꾸고 변경된 행 수를 반환한다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
update ActionItem a
   set a.approvalStatus = com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus.CANCELED,
       a.cancelReason = :cancelReason
 where a.documentId = :documentId
   and a.approvalStatus = com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus.PENDING
""")
    int cancelPendingByDocumentId(
            @Param("documentId") UUID documentId, @Param("cancelReason") String cancelReason);
}
