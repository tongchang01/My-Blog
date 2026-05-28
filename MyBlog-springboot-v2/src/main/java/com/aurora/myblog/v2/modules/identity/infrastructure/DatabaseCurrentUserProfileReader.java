package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.CurrentUserProfile;
import com.aurora.myblog.v2.modules.identity.domain.CurrentUserProfileReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DatabaseCurrentUserProfileReader implements CurrentUserProfileReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCurrentUserProfileReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
