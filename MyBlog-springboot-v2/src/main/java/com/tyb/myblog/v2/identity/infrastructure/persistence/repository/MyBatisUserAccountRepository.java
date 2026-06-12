package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.account.UserAccountRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserAccountEntity;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的登录账号持久化适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisUserAccountRepository implements UserAccountRepository {

    private final UserAccountMapper mapper;

    @Override
    public Optional<UserAccount> findActiveByUsername(String username) {
        return Optional.ofNullable(mapper.selectActiveByUsername(username))
                .map(this::toDomain);
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                AccountType.fromDatabaseValue(entity.getType()),
                entity.getTokenVersion(),
                entity.getLoginFailCount(),
                entity.getLockedUntil()
        );
    }
}
