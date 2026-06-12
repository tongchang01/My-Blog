package com.tyb.myblog.v2.identity.application.auth;

/**
 * 后台登录应用命令。
 *
 * @param username 未规范化的登录用户名
 * @param password 原样传递的明文密码
 * @param clientIp 可信客户端 IP，允许为空
 */
public record LoginCommand(String username, String password, String clientIp) {
}
