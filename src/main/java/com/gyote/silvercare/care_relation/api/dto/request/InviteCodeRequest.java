package com.gyote.silvercare.care_relation.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class InviteCodeRequest {

    @NotBlank(message = "초대 코드를 입력해 주세요.")
    private String inviteCode;

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}
