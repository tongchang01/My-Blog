package com.tyb.myblog.v2.identity.domain.auth;

import java.util.OptionalInt;

/**
 * 用户访问令牌版本查询端口。
 */
public interface UserTokenVersionRepository {

    OptionalInt findActiveTokenVersion(long userId);

    boolean incrementActiveTokenVersion(long userId);
}
