package com.tyb.myblog.v2.identity.infrastructure;

import com.tyb.myblog.v2.identity.domain.LoginAuditRecorder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
/**
 * 基于旧库认证表的登录审计记录器。
 *
 * <p>登录成功后更新 {@code t_user_auth.last_login_time} 和 {@code t_user_auth.ip_address}，
 * 用于后台安全审计和最近登录信息展示。</p>
 */
public class DatabaseLoginAuditRecorder implements LoginAuditRecorder {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseLoginAuditRecorder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 记录成功登录时间和客户端 IP。
     */
    @Override
    public void recordSuccessfulLogin(String authId, String clientIp) {
        jdbcTemplate.update("""
                        update t_user_auth
                        set last_login_time = current_timestamp,
                            ip_address = ?
                        where id = ?
                        """,
                clientIp,
                authId);
    }
}
