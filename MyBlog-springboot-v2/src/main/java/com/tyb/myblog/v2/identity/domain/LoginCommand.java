package com.tyb.myblog.v2.identity.domain;

/**
 * 登录命令。
 *
 * @param username 登录用户名
 * @param password 登录明文密码，只用于本次校验，禁止记录日志
 * @param clientIp 客户端 IP，用于登录审计
 */
public record LoginCommand(String username, String password, String clientIp) {
    /**
     * 创建不带客户端 IP 的登录命令，主要用于单元测试。
     */
    public LoginCommand(String username, String password) {
        this(username, password, null);
    }
}
