package com.gyote.silvercare.health_record.query.model;
import java.util.List;
public record HealthRecordPageView(List<HealthRecordView> items, String nextCursor, boolean hasNext) {}
