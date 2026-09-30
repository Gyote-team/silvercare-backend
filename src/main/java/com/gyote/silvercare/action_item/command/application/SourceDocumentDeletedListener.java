package com.gyote.silvercare.action_item.command.application;

import com.gyote.silvercare.action_item.domain.repository.ActionItemRepository;
import com.gyote.silvercare.medical_document.domain.MedicalDocumentDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 원본 문서 삭제 이벤트를 받아 그 문서의 PENDING 할 일 후보를 취소합니다(FC-13-00-06). */
@Component
@RequiredArgsConstructor
public class SourceDocumentDeletedListener {

    static final String CANCEL_REASON = "SOURCE_DOCUMENT_DELETED";

    private final ActionItemRepository actionItems;

    /** 원본 문서가 삭제되면 그 문서의 PENDING 할 일 후보를 삭제와 같은 트랜잭션에서 취소한다. */
    @EventListener
    @Transactional
    public void cancelPendingItems(MedicalDocumentDeletedEvent event) {
        actionItems.cancelPendingByDocumentId(event.documentId(), CANCEL_REASON);
    }
}
