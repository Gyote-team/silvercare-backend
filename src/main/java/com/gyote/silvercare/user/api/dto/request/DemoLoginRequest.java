package com.gyote.silvercare.user.api.dto.request;

import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.user.error.UserErrorCode;

public class DemoLoginRequest {

    private String account;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public DemoAccount toDemoAccount() {
        if (account == null || account.isBlank()) {
            throw new BusinessException(UserErrorCode.INVALID_ROLE);
        }
        try {
            return DemoAccount.valueOf(account);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(UserErrorCode.INVALID_ROLE);
        }
    }

    /** 독립적으로 시작하는 연결 테스트용 시연 계정입니다. */
    public enum DemoAccount {
        MINJI("demo-multirole-minji", "김민지"),
        SOONJA("demo-multirole-soonja", "김순자"),
        HWANWOO("demo-multirole-hwanwoo", "최환우");

        private final String kakaoIdSuffix;
        private final String displayName;

        DemoAccount(String kakaoIdSuffix, String displayName) {
            this.kakaoIdSuffix = kakaoIdSuffix;
            this.displayName = displayName;
        }

        public String kakaoId() {
            return User.DEMO_KAKAO_ID_PREFIX + kakaoIdSuffix;
        }

        public String displayName() {
            return displayName;
        }
    }
}
