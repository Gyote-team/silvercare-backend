package com.gyote.silvercare.action_item.domain.repository;

import com.gyote.silvercare.action_item.domain.entity.ActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ActionItemRepository extends JpaRepository<ActionItem, UUID> {

    /** 문서에서 나온 PENDING 후보를 모두 CANCELED로 바꾸고 변경된 행 수를 반환한다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ActionItem a
               set a.approvalStatus = com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus.CANCELED,
                   a.cancelReason = :cancelReason
             where a.documentId = :documentId
               and a.approvalStatus = com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus.PENDING
            """)
    int cancelPendingByDocumentId(
            @Param("documentId") UUID documentId,
            @Param("cancelReason") String cancelReason
    );
}
