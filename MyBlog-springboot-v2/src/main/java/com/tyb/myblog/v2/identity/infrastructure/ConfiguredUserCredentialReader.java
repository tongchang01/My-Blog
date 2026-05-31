package com.tyb.myblog.v2.identity.infrastructure;

import com.tyb.myblog.v2.identity.domain.AuthRole;
import com.tyb.myblog.v2.identity.domain.UserCredentialReader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Profile("configured-identity")
@Component
@EnableConfigurationProperties(ConfiguredIdentityProperties.class)
/**
 * 基于配置文件的用户凭证读取器。
 *
 * <p>只在 {@code configured-identity} profile 下启用，用于测试或临时环境。
 * 数据库登录读取器仍是长期主路径。</p>
 */
public class ConfiguredUserCredentialReader implements UserCredentialReader {

    private final ConfiguredIdentityProperties properties;

    public ConfiguredUserCredentialReader(ConfiguredIdentityProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建单用户读取器，供单元测试快速构造登录凭证。
     */
    public static ConfiguredUserCredentialReader singleUser(String username, String passwordHash, List<AuthRole> roles) {
        ConfiguredIdentityProperties.User user =
                new ConfiguredIdentityProperties.User("test-user", username, passwordHash, roles);
        return new ConfiguredUserCredentialReader(new ConfiguredIdentityProperties(List.of(user)));
    }

    /**
     * 按用户名忽略大小写匹配配置用户。
     */
    @Override
    public Optional<UserCredential> findByUsername(String username) {
        return properties.users().stream()
                .filter(user -> user.username().equalsIgnoreCase(username))
                .findFirst()
                .map(user -> new UserCredential(user.id(), user.username(), user.passwordHash(), user.roles()));
    }
}
