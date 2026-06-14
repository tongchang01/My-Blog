package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.ChangeablePasswordAccount;
import com.tyb.myblog.v2.identity.domain.account.PasswordAccountRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 基于 MyBatis XML 的修改密码账号持久化适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisPasswordAccountRepository
        implements PasswordAccountRepository {

    private final UserAccountMapper mapper;

    @Override
    public Optional<ChangeablePasswordAccount> findActiveByIdForUpdate(
            long userId) {
        return Optional.ofNullable(
                        mapper.selectActivePasswordAccountForUpdate(userId))
                .map(entity -> new ChangeablePasswordAccount(
                        entity.getId(),
                        AccountType.fromDatabaseValue(entity.getType()),
                        entity.getPasswordHash()));
    }

    @Override
    public boolean updatePasswordAndIncrementTokenVersion(
            long userId,
            String passwordHash,
            LocalDateTime updatedAt,
            Long updatedBy) {
        return mapper.updatePasswordAndIncrementTokenVersion(
                userId, passwordHash, updatedAt, updatedBy) == 1;
    }
}
