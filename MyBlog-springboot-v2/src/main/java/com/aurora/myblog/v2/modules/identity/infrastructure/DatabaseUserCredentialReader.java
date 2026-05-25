package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.UserCredentialReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DatabaseUserCredentialReader implements UserCredentialReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserCredentialReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserCredential> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        List<AccountRow> accounts = jdbcTemplate.query("""
                        select
                            ua.id as auth_id,
                            ua.username as username,
                            ua.password as password_hash
                        from t_user_auth ua
                        join t_user_info ui on ua.user_info_id = ui.id
                        where lower(ua.username) = lower(?)
                          and ui.is_disable = 0
                        limit 1
                        """,
                (rs, rowNum) -> new AccountRow(
                        rs.getString("auth_id"),
                        rs.getString("username"),
                        rs.getString("password_hash")),
                username.trim());
        if (accounts.isEmpty()) {
            return Optional.empty();
        }
        AccountRow account = accounts.get(0);
        List<AuthRole> roles = RoleNameMapper.toAuthRoles(loadRoleNames(account.id()));
        return Optional.of(new UserCredential(account.id(), account.username(), account.passwordHash(), roles));
    }

    private List<String> loadRoleNames(String authId) {
        return jdbcTemplate.query("""
                        select r.role_name
                        from t_user_auth ua
                        join t_user_role ur on ua.user_info_id = ur.user_id
                        join t_role r on ur.role_id = r.id
                        where ua.id = ?
                          and r.is_disable = 0
                        order by r.id
                        """,
                (rs, rowNum) -> rs.getString("role_name"),
                authId);
    }

    private record AccountRow(String id, String username, String passwordHash) {
    }
}
