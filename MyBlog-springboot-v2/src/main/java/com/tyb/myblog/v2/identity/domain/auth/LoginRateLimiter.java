package com.tyb.myblog.v2.identity.domain.auth;

/**
 * 后台登录连续失败限流端口。
 */
public interface LoginRateLimiter {

    /**
     * 判断当前 IP 与用户名组合是否处于冷却期。
     *
     * @param clientIp 可信客户端 IP，允许为空
     * @param normalizedUsername 已规范化的登录用户名
     * @return 达到失败阈值且冷却尚未结束时返回 {@code true}
     */
    boolean isBlocked(String clientIp, String normalizedUsername);

    /**
     * 原子记录一次已确认的凭据失败。
     *
     * @param clientIp 可信客户端 IP，允许为空
     * @param normalizedUsername 已规范化的登录用户名
     */
    void recordFailure(String clientIp, String normalizedUsername);

    /**
     * 登录凭据成功后清除当前组合的连续失败状态。
     *
     * @param clientIp 可信客户端 IP，允许为空
     * @param normalizedUsername 已规范化的登录用户名
     */
    void reset(String clientIp, String normalizedUsername);
}
