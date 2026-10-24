package com.gyote.silvercare.user.command.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.UserRole;
import com.gyote.silvercare.user.domain.UserStatus;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import com.gyote.silvercare.user.error.UserErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 한 사람이 가진 개인 계정과 보호자 계정 사이의 전환을 처리한다.
 * 계정마다 역할은 하나로 고정되고, 전환할 역할의 계정이 없으면 같은 그룹에 새로 만든다.
 */
@Service
public class AccountSwitchService {

    private final UserRepository users;
    private final UserAccountService accounts;

    public AccountSwitchService(UserRepository users, UserAccountService accounts) {
        this.users = users;
        this.accounts = accounts;
    }

    /** 현재 계정과 같은 사람의 활성 계정 목록을 반환한다. */
    @Transactional(readOnly = true)
    public List<User> linkedAccounts(User me) {
        return users.findByAccountGroupIdAndStatusOrderByRoleAsc(me.getAccountGroupId(), UserStatus.ACTIVE);
    }

    /** 대상 역할의 계정으로 전환할 계정을 반환한다. 없으면 현재 이름으로 새로 만들고, 개인 계정이면 초대 코드도 만든다. */
    @Transactional
    public User switchTo(String kakaoId, UserRole target) {
        if (target != UserRole.PATIENT && target != UserRole.CAREGIVER) {
            throw new BusinessException(UserErrorCode.INVALID_ROLE);
        }
        User me = requireActive(kakaoId);
        if (me.getRole() != UserRole.PATIENT && me.getRole() != UserRole.CAREGIVER) {
            throw new BusinessException(UserErrorCode.ROLE_NOT_SELECTED);
        }
        if (me.getRole() == target) {
            return me;
        }
        List<User> group = users.findAllByAccountGroupIdForUpdate(me.getAccountGroupId());
        return group.stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE && user.getRole() == target)
                .findFirst()
                .orElseGet(() -> createLinkedAccount(group, me, target));
    }

    private User createLinkedAccount(List<User> group, User me, UserRole target) {
        User owner = group.stream()
                .filter(User::isGroupOwner)
                .filter(user -> user.getKakaoId() != null)
                .findFirst()
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        User created = users.save(User.linkedAccount(owner, me.getName(), target));
        if (target == UserRole.PATIENT) {
            accounts.ensurePatientProfile(created);
        }
        return created;
    }

    private User requireActive(String kakaoId) {
        return users.findByKakaoId(kakaoId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }
}
