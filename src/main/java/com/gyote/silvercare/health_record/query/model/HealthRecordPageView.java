package com.gyote.silvercare.health_record.query.model;

import java.util.List;

/** 대상 개인의 건강기록 목록 및 커서 조회 결과. */
public record HealthRecordPageView(
        List<HealthRecordView> items, String nextCursor, boolean hasNext) {}
