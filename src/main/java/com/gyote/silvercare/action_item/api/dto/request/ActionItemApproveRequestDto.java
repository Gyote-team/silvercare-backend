package com.gyote.silvercare.action_item.api.dto.request;

import com.gyote.silvercare.schedule.api.dto.request.ScheduleWriteRequestDto;

import java.util.List;

public record ActionItemApproveRequestDto(List<ScheduleWriteRequestDto> schedules) {}
