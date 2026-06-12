package com.tyb.myblog.v2.identity.domain.auth;

/**
 * 密码摘要校验端口。
 */
public interface PasswordHashVerifier {

    /**
     * 校验明文密码是否匹配密码摘要。
     *
     * @param rawPassword 明文密码
     * @param passwordHash 密码摘要
     * @return 匹配时返回 {@code true}
     */
    boolean matches(String rawPassword, String passwordHash);
}
