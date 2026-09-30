package com.gyote.silvercare.user.api.dto.response;

import com.gyote.silvercare.user.domain.UserStatus;

/** 탈퇴 처리 결과다. */
public record WithdrawalResponse(UserStatus userStatus, int revokedRelationCount) {
}
