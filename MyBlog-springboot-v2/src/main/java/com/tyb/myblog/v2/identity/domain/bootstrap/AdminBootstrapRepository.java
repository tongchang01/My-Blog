package com.tyb.myblog.v2.identity.domain.bootstrap;

import com.tyb.myblog.v2.identity.domain.account.UserAccount;

/**
 * 首个管理员初始化的持久化端口。
 */
public interface AdminBootstrapRepository {

    /**
     * 判断是否存在未删除的管理员账号。
     *
     * @return 存在有效管理员时返回 {@code true}
     */
    boolean existsActiveAdmin();

    /**
     * 创建首个管理员账号。
     *
     * @param username 登录用户名
     * @param passwordHash BCrypt 密码摘要
     * @return 已持久化的管理员账号
     */
    UserAccount createAdmin(String username, String passwordHash);
}
