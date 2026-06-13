package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.CurrentAccount;
import com.tyb.myblog.v2.identity.domain.account.CurrentAccountRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserAccountEntity;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的当前账号只读仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisCurrentAccountRepository
        implements CurrentAccountRepository {

    private final UserAccountMapper mapper;

    @Override
    public Optional<CurrentAccount> findActiveById(long userId) {
        return Optional.ofNullable(mapper.selectActiveById(userId))
                .map(this::toDomain);
    }

    private CurrentAccount toDomain(UserAccountEntity entity) {
        return new CurrentAccount(
                entity.getId(),
                entity.getUsername(),
                AccountType.fromDatabaseValue(entity.getType()));
    }
}
