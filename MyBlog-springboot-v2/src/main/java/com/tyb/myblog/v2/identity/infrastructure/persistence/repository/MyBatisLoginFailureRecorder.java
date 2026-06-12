package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.identity.domain.auth.LoginFailureRecorder;
import com.tyb.myblog.v2.identity.domain.auth.LoginStateUpdateException;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 基于 MyBatis 的登录密码失败状态记录器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisLoginFailureRecorder implements LoginFailureRecorder {

    private final UserAccountMapper userAccountMapper;

    @Override
    public void recordPasswordFailure(
            long userId,
            LocalDateTime failedAt,
            int maxAttempts,
            LocalDateTime lockedUntil
    ) {
        int updatedRows = userAccountMapper.recordPasswordFailure(
                userId,
                failedAt,
                maxAttempts,
                lockedUntil
        );
        if (updatedRows != 1) {
            throw LoginStateUpdateException.passwordFailure(userId);
        }
    }
}
