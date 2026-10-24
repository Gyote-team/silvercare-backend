package com.gyote.silvercare.user.api.dto.request;

import jakarta.validation.constraints.NotNull;

/** 회원 탈퇴 의사 확인 요청이다. */
public class WithdrawalRequest {

    @NotNull
    private Boolean confirmed;

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }
}
