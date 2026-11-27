package com.gyote.silvercare.action_item.query.application;

import com.gyote.silvercare.action_item.domain.ActionItemAccessPolicy;
import com.gyote.silvercare.action_item.domain.ActionItemApprovalStatus;
import com.gyote.silvercare.action_item.domain.repository.ActionItemQueryRepository;
import com.gyote.silvercare.action_item.error.ActionItemErrorCode;
import com.gyote.silvercare.action_item.query.model.ActionItemPageView;
import com.gyote.silvercare.action_item.query.model.ActionItemRow;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.user.domain.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** 조회 시점의 대상 권한과 필터에 묶인 커서로 후보를 반환한다. */
@Service
@Transactional(readOnly = true)
public class ActionItemQueryService {
    private final ActionItemQueryRepository items;
    private final ActionItemAccessPolicy access;

    public ActionItemQueryService(ActionItemQueryRepository items, ActionItemAccessPolicy access) {
        this.items = items;
        this.access = access;
    }

    /** 대상 생략은 개인 본인으로 해석하고 모든 상태 또는 선택 상태의 최신 후보를 조회한다. */
    public ActionItemPageView list(
            User actor,
            UUID patientId,
            ActionItemApprovalStatus status,
            String cursor,
            Integer requestedSize) {
        UUID target = access.requirePatient(actor, patientId);
        int size = requestedSize == null ? 20 : requestedSize;
        if (size < 1 || size > 100) {
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        }
        Position position = decode(cursor, target, status);
        List<ActionItemRow> rows =
                items.findPage(target, status, position.createdAt(), position.id(), size + 1);
        List<ActionItemRow> shown = rows.subList(0, Math.min(size, rows.size()));
        String next =
                rows.size() > size ? encode(target, status, shown.get(shown.size() - 1)) : null;
        return new ActionItemPageView(shown, next);
    }

    /** 후보 소유 개인의 현재 권한을 확인하고 상세와 검증된 출처를 반환한다. */
    public ActionItemRow detail(User actor, UUID actionItemId) {
        ActionItemRow row =
                items.findById(actionItemId)
                        .orElseThrow(() -> new BusinessException(ActionItemErrorCode.NOT_FOUND));
        access.requirePatient(actor, row.patientId());
        return row;
    }

    private Position decode(String cursor, UUID target, ActionItemApprovalStatus status) {
        if (cursor == null) return new Position(null, null);
        try {
            if (cursor.length() > 512) throw new IllegalArgumentException();
            String[] values =
                    new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
                            .split("\\|", -1);
            if (values.length != 4
                    || !target.toString().equals(values[0])
                    || !filter(status).equals(values[1])) {
                throw new IllegalArgumentException();
            }
            return new Position(Instant.parse(values[2]), UUID.fromString(values[3]));
        } catch (RuntimeException invalid) {
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        }
    }

    private String encode(UUID target, ActionItemApprovalStatus status, ActionItemRow last) {
        String value =
                target + "|" + filter(status) + "|" + last.createdAt() + "|" + last.actionItemId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String filter(ActionItemApprovalStatus status) {
        return status == null ? "ALL" : status.name();
    }

    private record Position(Instant createdAt, UUID id) {}
}
