package com.gyote.silvercare.schedule.api.mapper;

import com.gyote.silvercare.schedule.api.dto.response.SchedulePageResponseDto;
import com.gyote.silvercare.schedule.api.dto.response.ScheduleResponseDto;
import com.gyote.silvercare.schedule.query.model.SchedulePageView;
import com.gyote.silvercare.schedule.query.model.ScheduleRow;

import org.springframework.stereotype.Component;

@Component
public class ScheduleResponseMapper {
    /** 일정 조회 모델을 공개 응답으로 변환한다. */
    public ScheduleResponseDto toResponse(ScheduleRow r) {
        return new ScheduleResponseDto(
                r.scheduleId(),
                r.patientId(),
                r.actionItemId(),
                r.title(),
                r.scheduleType(),
                r.startsAt(),
                r.endsAt(),
                r.status(),
                r.repeatFrequency(),
                r.repeatInterval(),
                r.repeatUntil(),
                r.nextOccurrenceAt(),
                r.version());
    }

    /** 페이지 조회 모델을 공개 목록 응답으로 변환한다. */
    public SchedulePageResponseDto toResponse(SchedulePageView r) {
        return new SchedulePageResponseDto(
                r.items().stream().map(this::toResponse).toList(),
                r.page(),
                r.size(),
                r.totalCount());
    }
}
