package com.tyb.myblog.v2.identity.domain;

/**
 * 登录审计记录端口。
 */
public interface LoginAuditRecorder {
    /**
     * 记录成功登录信息。
     *
     * @param authId   认证账号 ID
     * @param clientIp 客户端 IP
     */
    void recordSuccessfulLogin(String authId, String clientIp);
}
