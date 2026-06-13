package com.tyb.myblog.v2.identity.domain.account;

import java.util.Optional;

/**
 * 当前账号只读仓储端口。
 */
public interface CurrentAccountRepository {

    /**
     * 按账号 ID 查询未删除账号的安全投影。
     *
     * @param userId 账号 ID
     * @return 未删除账号，不存在时返回空
     */
    Optional<CurrentAccount> findActiveById(long userId);
}
