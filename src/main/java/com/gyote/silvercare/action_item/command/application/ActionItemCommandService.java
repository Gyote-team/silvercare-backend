package com.gyote.silvercare.action_item.command.application;

import com.gyote.silvercare.action_item.domain.ActionItemAccessPolicy;
import com.gyote.silvercare.action_item.domain.ActionItemType;
import com.gyote.silvercare.action_item.domain.entity.ActionItem;
import com.gyote.silvercare.action_item.domain.repository.ActionItemRepository;
import com.gyote.silvercare.action_item.error.ActionItemErrorCode;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.medical_document.domain.repository.MedicalDocumentRepository;
import com.gyote.silvercare.schedule.domain.ScheduleInput;
import com.gyote.silvercare.schedule.domain.ScheduleType;
import com.gyote.silvercare.schedule.domain.entity.Schedule;
import com.gyote.silvercare.schedule.domain.repository.ScheduleRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.visit.domain.VisitStatus;
import com.gyote.silvercare.visit.domain.repository.VisitRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 문서→후보 잠금 순서를 지키며 승인과 일정 생성을 원자적으로 처리한다. */
@Service
@Transactional
public class ActionItemCommandService {
    private final ActionItemRepository items;
    private final MedicalDocumentRepository documents;
    private final VisitRepository visits;
    private final ScheduleRepository schedules;
    private final ActionItemAccessPolicy access;

    public ActionItemCommandService(
            ActionItemRepository i,
            MedicalDocumentRepository d,
            VisitRepository v,
            ScheduleRepository s,
            ActionItemAccessPolicy a) {
        items = i;
        documents = d;
        visits = v;
        schedules = s;
        access = a;
    }

    /** 대기 후보 승인과 최대 8개 복약 시간대 생성을 같은 트랜잭션에서 완료한다. */
    public List<UUID> approve(User actor, UUID id, List<ScheduleInput> inputs) {
        Locked row = require(actor, id);
        ActionItem item = row.item();
        item.requirePending();
        if (row.sourceDeleted() || row.visitCanceled())
            throw new BusinessException(ActionItemErrorCode.STATE_CONFLICT);
        if (inputs == null
                || inputs.isEmpty()
                || inputs.size() > 8
                || (item.getType() != ActionItemType.MEDICATION && inputs.size() != 1))
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        ScheduleType expected =
                switch (item.getType()) {
                    case MEDICATION -> ScheduleType.MEDICATION;
                    case REVISIT -> ScheduleType.REVISIT;
                    case TEST -> ScheduleType.TEST;
                    default -> ScheduleType.OTHER;
                };
        Set<java.time.Instant> times = new HashSet<>();
        for (ScheduleInput input : inputs) {
            Schedule.validate(input);
            if (input.scheduleType() != expected || !times.add(input.startsAt()))
                throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        }
        List<UUID> ids = new ArrayList<>();
        for (int slot = 0; slot < inputs.size(); slot++)
            ids.add(
                    schedules
                            .save(Schedule.create(row.patientId(), id, slot, inputs.get(slot)))
                            .getId());
        item.approve(actor.getId());
        schedules.flush();
        items.flush();
        return List.copyOf(ids);
    }

    /** 대기 후보를 거절하며 일정은 만들지 않는다. */
    public void reject(User actor, UUID id) {
        require(actor, id).item().reject(actor.getId());
        items.flush();
    }

    private Locked require(User actor, UUID id) {
        UUID documentId =
                items.findDocumentId(id)
                        .orElseThrow(() -> new BusinessException(ActionItemErrorCode.NOT_FOUND));
        var document =
                documents
                        .findLockedById(documentId)
                        .orElseThrow(() -> new BusinessException(ActionItemErrorCode.NOT_FOUND));
        var item =
                items.findLockedById(id)
                        .orElseThrow(() -> new BusinessException(ActionItemErrorCode.NOT_FOUND));
        var visit =
                visits.findByIdAndDeletedAtIsNull(item.getVisitId())
                        .orElseThrow(() -> new BusinessException(ActionItemErrorCode.NOT_FOUND));
        if (!item.getVisitId().equals(document.getVisitId())
                || !visit.getPatientId().equals(document.getPatientId()))
            throw new BusinessException(ActionItemErrorCode.NOT_FOUND);
        access.requirePatient(actor, visit.getPatientId());
        return new Locked(
                item,
                visit.getPatientId(),
                document.isDeleted() || document.getDeletedAt() != null,
                visit.getStatus() == VisitStatus.CANCELED);
    }

    private record Locked(
            ActionItem item, UUID patientId, boolean sourceDeleted, boolean visitCanceled) {}
}
