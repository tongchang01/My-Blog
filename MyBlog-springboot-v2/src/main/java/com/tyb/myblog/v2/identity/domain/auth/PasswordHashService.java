package com.tyb.myblog.v2.identity.domain.auth;

/**
 * 后台账号密码摘要服务。
 */
public interface PasswordHashService {

    /**
     * 校验明文密码与已保存摘要是否匹配。
     *
     * @param rawPassword 明文密码
     * @param passwordHash 已保存的密码摘要
     * @return 匹配时返回 true
     */
    boolean matches(String rawPassword, String passwordHash);

    /**
     * 使用当前安全配置生成不可逆密码摘要。
     *
     * @param rawPassword 明文密码
     * @return 不可逆密码摘要
     */
    String encode(String rawPassword);
}
