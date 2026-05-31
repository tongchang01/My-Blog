package com.tyb.myblog.v2.identity.infrastructure;

import com.tyb.myblog.v2.identity.domain.CurrentUserProfile;
import com.tyb.myblog.v2.identity.domain.CurrentUserProfileReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
/**
 * 基于旧库用户表的当前用户资料读取器。
 *
 * <p>从 {@code t_user_auth} 和 {@code t_user_info} 联表读取当前用户资料，
 * 并过滤 {@code t_user_info.is_disable = 0} 的启用用户。</p>
 */
public class DatabaseCurrentUserProfileReader implements CurrentUserProfileReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCurrentUserProfileReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据认证账号 ID 查询当前用户资料。
     */
    @Override
    public Optional<CurrentUserProfile> findByAuthId(String authId) {
        if (authId == null || authId.isBlank()) {
            return Optional.empty();
        }
        List<CurrentUserProfile> profiles = jdbcTemplate.query("""
                        select
                            ua.id as auth_id,
                            ui.id as user_info_id,
                            ua.username as username,
                            ui.nickname as nickname,
                            ui.avatar as avatar,
                            ui.email as email
                        from t_user_auth ua
                        join t_user_info ui on ua.user_info_id = ui.id
                        where ua.id = ?
                          -- 旧库使用 is_disable = 0 表示用户资料可用。
                          and ui.is_disable = 0
                        limit 1
                        """,
                (rs, rowNum) -> new CurrentUserProfile(
                        rs.getString("auth_id"),
                        rs.getString("user_info_id"),
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("email")),
                authId.trim());
        return profiles.stream().findFirst();
    }
}
