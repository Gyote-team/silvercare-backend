package com.gyote.silvercare.action_item.query.model;

import java.util.List;

/** 최신순 후보 목록과 다음 페이지 커서를 반환한다. */
public record ActionItemPageView(List<ActionItemRow> items, String nextCursor) {}
