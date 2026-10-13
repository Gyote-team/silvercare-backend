package com.gyote.silvercare.visit.query.model;

import java.util.List;

/** 방문 커서 목록과 다음 페이지 정보. */
public record VisitPageView(List<VisitView> items, String nextCursor, boolean hasNext) {}
