package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.LoginAuditRecorder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseLoginAuditRecorder implements LoginAuditRecorder {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseLoginAuditRecorder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
