package com.gyote.silvercare.schedule.domain;

import com.gyote.silvercare.action_item.domain.ActionItemAccessPolicy;
import com.gyote.silvercare.action_item.error.ActionItemErrorCode;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.schedule.error.ScheduleErrorCode;
import com.gyote.silvercare.user.domain.User;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScheduleAccessPolicy {
    private final ActionItemAccessPolicy access;

    public ScheduleAccessPolicy(ActionItemAccessPolicy access) {
        this.access = access;
    }

    /** 본인 또는 ACTIVE 보호자에게만 일정 조회·변경을 허용한다. */
    public UUID requirePatient(User actor, UUID patientId) {
        try {
            return access.requirePatient(actor, patientId);
        } catch (BusinessException error) {
            if (error.getErrorCode() == ActionItemErrorCode.ACCESS_DENIED)
                throw new BusinessException(ScheduleErrorCode.ACCESS_DENIED);
            throw error;
        }
    }
}
