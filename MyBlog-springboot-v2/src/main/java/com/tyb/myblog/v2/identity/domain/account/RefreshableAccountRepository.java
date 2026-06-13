package com.tyb.myblog.v2.identity.domain.account;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 查询可刷新认证会话的后台账号。
 */
public interface RefreshableAccountRepository {

    Optional<RefreshableAccount> findRefreshableById(
            long userId,
            LocalDateTime now
    );
}
