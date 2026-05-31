package com.tyb.myblog.v2.identity;

import com.tyb.myblog.v2.identity.infrastructure.DatabaseLoginAuditRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseLoginAuditRecorder.class)
class DatabaseLoginAuditRecorderTest {

    @Autowired
    private DatabaseLoginAuditRecorder recorder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void updatesLastLoginTimeAndIpAddress() {
        recorder.recordSuccessfulLogin("1", "203.0.113.10");

        AuditRow row = loadAuditRow("1");
        assertThat(row.lastLoginTime()).isNotNull();
        assertThat(row.ipAddress()).isEqualTo("203.0.113.10");
        assertThat(row.ipSource()).isNull();
    }

    @Test
    void allowsNullIpAddressWithoutChangingIpSource() {
        jdbcTemplate.update("update t_user_auth set ip_source = ? where id = ?", "保留归属地", 1);

        recorder.recordSuccessfulLogin("1", null);

        AuditRow row = loadAuditRow("1");
        assertThat(row.lastLoginTime()).isNotNull();
        assertThat(row.ipAddress()).isNull();
        assertThat(row.ipSource()).isEqualTo("保留归属地");
    }

    private AuditRow loadAuditRow(String authId) {
        return jdbcTemplate.queryForObject("""
                        select last_login_time, ip_address, ip_source
                        from t_user_auth
                        where id = ?
                        """,
                (rs, rowNum) -> new AuditRow(
                        rs.getTimestamp("last_login_time"),
                        rs.getString("ip_address"),
                        rs.getString("ip_source")),
                authId);
    }

    private record AuditRow(java.sql.Timestamp lastLoginTime, String ipAddress, String ipSource) {
    }
}
