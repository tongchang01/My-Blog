package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccount;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccountRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 基于 MyBatis 的可刷新账号查询实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisRefreshableAccountRepository
        implements RefreshableAccountRepository {

    private final UserAccountMapper userAccountMapper;

    @Override
    public Optional<RefreshableAccount> findRefreshableById(
            long userId,
            LocalDateTime now
    ) {
        return Optional.ofNullable(
                        userAccountMapper.selectRefreshableById(userId, now)
                )
                .map(entity -> new RefreshableAccount(
                        entity.getId(),
                        entity.getUsername(),
                        AccountType.fromDatabaseValue(entity.getType()),
                        entity.getTokenVersion()
                ));
    }
}
