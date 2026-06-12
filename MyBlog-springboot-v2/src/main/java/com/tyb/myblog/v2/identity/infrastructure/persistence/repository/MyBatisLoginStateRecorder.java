package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.identity.domain.auth.LoginStateRecorder;
import com.tyb.myblog.v2.identity.domain.auth.LoginStateUpdateException;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 基于 MyBatis 的后台登录状态记录器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisLoginStateRecorder implements LoginStateRecorder {

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

    @Override
    public void recordSuccessfulLogin(long userId, LocalDateTime loggedInAt, String clientIp) {
        int updatedRows = userAccountMapper.recordSuccessfulLogin(userId, loggedInAt, clientIp);
        if (updatedRows != 1) {
            throw LoginStateUpdateException.successfulLogin(userId);
        }
    }
}
