package com.tyb.myblog.v2.identity.infrastructure;

import com.tyb.myblog.v2.identity.domain.AuthRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 配置文件中的备用身份配置。
 *
 * <p>绑定 {@code myblog.identity}，主要用于测试或特殊环境下通过配置提供登录账号。
 * 生产环境应优先使用数据库账号，避免把真实密码摘要长期维护在配置文件中。</p>
 *
 * @param users 配置用户列表
 */
@ConfigurationProperties("myblog.identity")
public record ConfiguredIdentityProperties(List<User> users) {

    /**
     * 复制配置用户列表，避免运行期被外部集合修改。
     */
    public ConfiguredIdentityProperties {
        users = users == null ? List.of() : List.copyOf(users);
    }

    /**
     * 配置用户。
     *
     * @param id           用户 ID
     * @param username     登录用户名
     * @param passwordHash BCrypt 密码摘要
     * @param roles        用户角色
     */
    public record User(String id, String username, String passwordHash, List<AuthRole> roles) {
        /**
         * 复制角色列表。
         */
        public User {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
