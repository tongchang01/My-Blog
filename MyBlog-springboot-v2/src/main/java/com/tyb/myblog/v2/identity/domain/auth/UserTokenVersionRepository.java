package com.tyb.myblog.v2.identity.domain.auth;

import java.time.LocalDateTime;
import java.util.OptionalInt;

/**
 * 后台账号 access token 版本仓储端口。
 */
public interface UserTokenVersionRepository {

    OptionalInt findActiveTokenVersion(long userId);

    boolean incrementActiveTokenVersion(
            long userId,
            LocalDateTime updatedAt,
            Long updatedBy
    );
}
