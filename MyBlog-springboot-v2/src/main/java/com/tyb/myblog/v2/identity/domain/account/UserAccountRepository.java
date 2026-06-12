package com.tyb.myblog.v2.identity.domain.account;

import java.util.Optional;

/**
 * 登录账号仓储端口。
 */
public interface UserAccountRepository {

    /**
     * 按用户名查找未删除账号。
     *
     * @param username 登录用户名
     * @return 匹配的未删除账号，不存在时返回空
     */
    Optional<UserAccount> findActiveByUsername(String username);
}
