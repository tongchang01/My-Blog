package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.identity.domain.auth.UserTokenVersionRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserTokenVersionMapper;
import org.springframework.stereotype.Repository;

import java.util.OptionalInt;

@Repository
public class MyBatisUserTokenVersionRepository implements UserTokenVersionRepository {

    private final UserTokenVersionMapper mapper;

    public MyBatisUserTokenVersionRepository(UserTokenVersionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public OptionalInt findActiveTokenVersion(long userId) {
        Integer tokenVersion = mapper.selectActiveTokenVersion(userId);
        return tokenVersion == null ? OptionalInt.empty() : OptionalInt.of(tokenVersion);
    }
}
