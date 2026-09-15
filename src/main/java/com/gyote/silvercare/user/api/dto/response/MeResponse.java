package com.gyote.silvercare.user.api.dto.response;

public record MeResponse(
        String id,
        String name,
        String role,
        String status,
        String inviteCode
) {
}
