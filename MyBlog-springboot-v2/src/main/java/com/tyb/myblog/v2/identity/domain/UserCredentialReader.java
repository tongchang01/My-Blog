package com.tyb.myblog.v2.identity.domain;

import java.util.List;
import java.util.Optional;

/**
 * 用户登录凭证读取端口。
 */
public interface UserCredentialReader {
    /**
     * 按用户名查询可登录账号。
     */
    Optional<UserCredential> findByUsername(String username);

    /**
     * 用户登录凭证。
     *
     * @param id           认证账号 ID
     * @param username     登录用户名
     * @param passwordHash BCrypt 密码摘要
     * @param roles        用户角色
     */
    record UserCredential(String id, String username, String passwordHash, List<AuthRole> roles) {
    }
}
