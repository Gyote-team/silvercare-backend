package com.gyote.silvercare.schedule.query.model;

import java.util.List;

public record SchedulePageView(List<ScheduleRow> items, int page, int size, long totalCount) {}
