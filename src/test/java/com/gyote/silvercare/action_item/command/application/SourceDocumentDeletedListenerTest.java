package com.gyote.silvercare.action_item.command.application;

import com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus;
import com.gyote.silvercare.action_item.domain.entity.ActionItem;
import com.gyote.silvercare.action_item.domain.repository.ActionItemRepository;
import com.gyote.silvercare.medical_document.domain.MedicalDocumentDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SourceDocumentDeletedListenerTest {

    @Autowired
    private ActionItemRepository actionItems;

    @Autowired
    private TestEntityManager em;

    private SourceDocumentDeletedListener listener;
    private UUID deletedDocumentId;
    private UUID otherDocumentId;

    @BeforeEach
    void setUp() {
        listener = new SourceDocumentDeletedListener(actionItems);
        deletedDocumentId = UUID.randomUUID();
        otherDocumentId = UUID.randomUUID();
    }

    @Test
    void deletedDocumentPendingItemsAreCanceled() {
        UUID itemId = saveItem(deletedDocumentId, "PENDING");

        deleteDocument(deletedDocumentId);

        ActionItem item = find(itemId);
        assertThat(item.getApprovalStatus()).isEqualTo(ActionItemApprovalStatus.CANCELED);
        assertThat(item.getCancelReason()).isEqualTo("SOURCE_DOCUMENT_DELETED");
        assertThat(item.getDocumentId()).isEqualTo(deletedDocumentId);
    }

    @Test
    void approvedItemsOfDeletedDocumentStay() {
        UUID itemId = saveItem(deletedDocumentId, "APPROVED");

        deleteDocument(deletedDocumentId);

        ActionItem item = find(itemId);
        assertThat(item.getApprovalStatus()).isEqualTo(ActionItemApprovalStatus.APPROVED);
        assertThat(item.getCancelReason()).isNull();
    }

    @Test
    void pendingItemsOfOtherDocumentsStay() {
        UUID itemId = saveItem(otherDocumentId, "PENDING");

        deleteDocument(deletedDocumentId);

        ActionItem item = find(itemId);
        assertThat(item.getApprovalStatus()).isEqualTo(ActionItemApprovalStatus.PENDING);
        assertThat(item.getCancelReason()).isNull();
    }

    private UUID saveItem(UUID documentId, String approvalStatus) {
        UUID itemId = UUID.randomUUID();
        em.getEntityManager().createNativeQuery("""
                        INSERT INTO action_items
                            (id, visit_id, document_id, type, title, approval_status, created_at)
                        VALUES (:id, :visitId, :documentId, 'MEDICATION', '혈압약 복용', :approvalStatus, :createdAt)
                        """)
                .setParameter("id", itemId)
                .setParameter("visitId", UUID.randomUUID())
                .setParameter("documentId", documentId)
                .setParameter("approvalStatus", approvalStatus)
                .setParameter("createdAt", Instant.parse("2026-09-22T01:00:00Z"))
                .executeUpdate();
        return itemId;
    }

    private void deleteDocument(UUID documentId) {
        listener.cancelPendingItems(new MedicalDocumentDeletedEvent(
                documentId, UUID.randomUUID(), UUID.randomUUID(), Instant.now()));
    }

    private ActionItem find(UUID itemId) {
        return actionItems.findById(itemId).orElseThrow();
    }
}
