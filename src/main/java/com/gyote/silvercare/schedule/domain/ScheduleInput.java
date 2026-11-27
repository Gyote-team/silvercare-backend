package com.gyote.silvercare.schedule.domain;

import java.time.Instant;
import java.time.LocalDate;

/** 단일 일정 또는 한 복약 시간대의 반복 규칙이다. 날짜는 한국 시간 기준이다. */
public record ScheduleInput(
        String title,
        ScheduleType scheduleType,
        Instant startsAt,
        Instant endsAt,
        RepeatFrequency repeatFrequency,
        Integer repeatInterval,
        LocalDate repeatUntil) {}
