package com.gyote.silvercare.schedule.command.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.schedule.domain.ScheduleAccessPolicy;
import com.gyote.silvercare.schedule.domain.ScheduleInput;
import com.gyote.silvercare.schedule.domain.ScheduleStatus;
import com.gyote.silvercare.schedule.domain.entity.Schedule;
import com.gyote.silvercare.schedule.domain.repository.ScheduleRepository;
import com.gyote.silvercare.schedule.error.ScheduleErrorCode;
import com.gyote.silvercare.user.domain.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ScheduleCommandService {
    private final ScheduleRepository schedules;
    private final ScheduleAccessPolicy access;

    public ScheduleCommandService(ScheduleRepository schedules, ScheduleAccessPolicy access) {
        this.schedules = schedules;
        this.access = access;
    }

    /** 후보 없이도 본인 또는 ACTIVE 보호자가 수동 일정을 생성한다. */
    public UUID create(User actor, UUID patientId, ScheduleInput input) {
        UUID target = access.requirePatient(actor, patientId);
        return schedules.saveAndFlush(Schedule.create(target, null, 0, input)).getId();
    }

    /** 최신 버전에서만 반복 전체와 완료·취소 상태를 수정한다. */
    public UUID update(
            User actor,
            UUID id,
            UUID patientId,
            ScheduleInput input,
            ScheduleStatus status,
            Long version) {
        Schedule row = require(actor, id);
        if (patientId != null && !patientId.equals(row.getPatientId()))
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        requireVersion(row, version);
        row.update(input, status);
        schedules.flush();
        return row.getId();
    }

    /** 반복 전체를 버전 확인 후 삭제하고 승인 후보는 재승인하지 않는다. */
    public void delete(User actor, UUID id, Long version) {
        Schedule row = require(actor, id);
        requireVersion(row, version);
        row.delete();
        schedules.flush();
    }

    private Schedule require(User actor, UUID id) {
        Schedule row =
                schedules
                        .findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new BusinessException(ScheduleErrorCode.NOT_FOUND));
        access.requirePatient(actor, row.getPatientId());
        return row;
    }

    private void requireVersion(Schedule row, Long version) {
        if (version == null || version < 0)
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        if (row.getVersion() != version) throw new BusinessException(ScheduleErrorCode.CONFLICT);
    }
}
