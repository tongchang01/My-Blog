package com.tyb.myblog.v2.identity.infrastructure;

import com.tyb.myblog.v2.identity.domain.AuthRole;
import com.tyb.myblog.v2.identity.domain.UserCredentialReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
/**
 * 基于旧库用户认证表的登录凭证读取器。
 *
 * <p>从 {@code t_user_auth} 读取用户名和密码摘要，从角色关联表读取用户角色。
 * 查询时会过滤被禁用的用户资料和被禁用的角色。</p>
 */
public class DatabaseUserCredentialReader implements UserCredentialReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserCredentialReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按用户名查询可登录账号和角色。
     */
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
                          -- 旧库使用 is_disable = 0 表示账号关联的用户资料可用。
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

    /**
     * 读取账号对应的启用角色名称。
     */
    private List<String> loadRoleNames(String authId) {
        return jdbcTemplate.query("""
                        select r.role_name
                        from t_user_auth ua
                        join t_user_role ur on ua.user_info_id = ur.user_id
                        join t_role r on ur.role_id = r.id
                        where ua.id = ?
                          -- 被禁用角色不能参与当前登录用户的权限计算。
                          and r.is_disable = 0
                        order by r.id
                        """,
                (rs, rowNum) -> rs.getString("role_name"),
                authId);
    }

    /**
     * 旧库认证账号行。
     */
    private record AccountRow(String id, String username, String passwordHash) {
    }
}
