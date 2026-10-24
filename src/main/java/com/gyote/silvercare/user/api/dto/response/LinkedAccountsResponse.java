package com.gyote.silvercare.user.api.dto.response;

import com.gyote.silvercare.user.domain.User;

import java.util.List;

/**
 * 같은 사람이 가진 계정 목록.
 * currentRole은 지금 로그인한 계정의 역할이고, accounts에는 활성 계정만 담긴다.
 */
public record LinkedAccountsResponse(
        String currentRole,
        List<Account> accounts
) {

    public record Account(String role, String name, boolean current) {
    }

    public static LinkedAccountsResponse of(User me, List<User> linked) {
        return new LinkedAccountsResponse(
                me.getRole().name(),
                linked.stream()
                        .map(user -> new Account(user.getRole().name(), user.getName(), user.getId().equals(me.getId())))
                        .toList()
        );
    }
}
