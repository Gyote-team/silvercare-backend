package com.gyote.silvercare.user.api.dto.response;

import com.gyote.silvercare.user.domain.User;

import java.time.Instant;

/**
 * 로그인한 사용자 정보.
 * loginProvider는 KAKAO 또는 시연용 계정인 DEMO이고, createdAt은 가입 시각이다.
 */
public record MeResponse(
        String id,
        String name,
        String role,
        String status,
        String inviteCode,
        boolean hasPatientProfile,
        String patientId,
        String loginProvider,
        Instant createdAt
) {

    public static MeResponse of(User user, String displayInviteCode) {
        return of(user, displayInviteCode, null);
    }

    public static MeResponse of(User user, String displayInviteCode, String patientId) {
        return new MeResponse(
                user.getId().toString(),
                user.getName(),
                user.getRole().name(),
                user.getStatus().name(),
                displayInviteCode,
                patientId != null,
                patientId,
                user.isDemoAccount() ? "DEMO" : "KAKAO",
                user.getCreatedAt()
        );
    }
}
