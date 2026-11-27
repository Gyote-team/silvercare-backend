package com.gyote.silvercare.action_item.api.dto.response;

import java.util.List;
import java.util.UUID;

public record ActionItemApprovalResponseDto(UUID actionItemId, List<UUID> scheduleIds) {}
