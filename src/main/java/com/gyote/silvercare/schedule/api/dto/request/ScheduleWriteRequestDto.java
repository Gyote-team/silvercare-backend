package com.gyote.silvercare.schedule.api.dto.request;

import com.gyote.silvercare.schedule.domain.RepeatFrequency;
import com.gyote.silvercare.schedule.domain.ScheduleInput;
import com.gyote.silvercare.schedule.domain.ScheduleStatus;
import com.gyote.silvercare.schedule.domain.ScheduleType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ScheduleWriteRequestDto(
        UUID patientId,
        String title,
        ScheduleType scheduleType,
        Instant startsAt,
        Instant endsAt,
        RepeatFrequency repeatFrequency,
        Integer repeatInterval,
        LocalDate repeatUntil,
        ScheduleStatus status,
        Long version) {
    /** HTTP 입력을 API와 독립된 일정 규칙으로 변환한다. */
    public ScheduleInput toInput() {
        return new ScheduleInput(
                title,
                scheduleType,
                startsAt,
                endsAt,
                repeatFrequency,
                repeatInterval,
                repeatUntil);
    }
}
