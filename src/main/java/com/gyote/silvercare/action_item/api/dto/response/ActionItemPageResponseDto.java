package com.gyote.silvercare.action_item.api.dto.response;

import java.util.List;

/** 후보 목록과 같은 대상·상태의 다음 페이지 커서를 반환한다. */
public record ActionItemPageResponseDto(List<ActionItemResponseDto> items, String nextCursor) {}
