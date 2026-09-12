package com.gyote.silvercare.user.api.request;

import com.gyote.silvercare.user.domain.UserRole;

public class RoleRequest {

    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UserRole toUserRole() {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("역할을 고르세요");
        }
        UserRole chosen = UserRole.valueOf(role);
        if (chosen != UserRole.PATIENT && chosen != UserRole.CAREGIVER) {
            throw new IllegalArgumentException("역할은 개인 또는 보호자만 고를 수 있습니다");
        }
        return chosen;
    }
}
