package com.gyote.silvercare.schedule.query.model;

import com.gyote.silvercare.schedule.domain.RepeatFrequency;
import com.gyote.silvercare.schedule.domain.ScheduleStatus;
import com.gyote.silvercare.schedule.domain.ScheduleType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ScheduleRow(
        UUID scheduleId,
        UUID patientId,
        UUID actionItemId,
        String title,
        ScheduleType scheduleType,
        Instant startsAt,
        Instant endsAt,
        ScheduleStatus status,
        RepeatFrequency repeatFrequency,
        int repeatInterval,
        LocalDate repeatUntil,
        Instant nextOccurrenceAt,
        long version) {}
