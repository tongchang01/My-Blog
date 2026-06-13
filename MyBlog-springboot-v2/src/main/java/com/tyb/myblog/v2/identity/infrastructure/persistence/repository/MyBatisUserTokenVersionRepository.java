package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.identity.domain.auth.UserTokenVersionRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserTokenVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.OptionalInt;

/**
 * 基于 MyBatis 的后台账号 token 版本仓储。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisUserTokenVersionRepository
        implements UserTokenVersionRepository {

    private final UserTokenVersionMapper mapper;

    @Override
    public OptionalInt findActiveTokenVersion(long userId) {
        Integer tokenVersion = mapper.selectActiveTokenVersion(userId);
        return tokenVersion == null
                ? OptionalInt.empty()
                : OptionalInt.of(tokenVersion);
    }

    @Override
    public boolean incrementActiveTokenVersion(
            long userId,
            LocalDateTime updatedAt,
            Long updatedBy
    ) {
        return mapper.incrementActiveTokenVersion(
                userId,
                updatedAt,
                updatedBy) == 1;
    }
}
