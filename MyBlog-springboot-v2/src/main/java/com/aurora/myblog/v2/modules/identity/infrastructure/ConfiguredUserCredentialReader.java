package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.UserCredentialReader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@EnableConfigurationProperties(ConfiguredIdentityProperties.class)
public class ConfiguredUserCredentialReader implements UserCredentialReader {

    private final ConfiguredIdentityProperties properties;

    public ConfiguredUserCredentialReader(ConfiguredIdentityProperties properties) {
        this.properties = properties;
    }

    public static ConfiguredUserCredentialReader singleUser(String username, String passwordHash, List<AuthRole> roles) {
        ConfiguredIdentityProperties.User user =
                new ConfiguredIdentityProperties.User("test-user", username, passwordHash, roles);
        return new ConfiguredUserCredentialReader(new ConfiguredIdentityProperties(List.of(user)));
    }

    @Override
    public Optional<UserCredential> findByUsername(String username) {
        return properties.users().stream()
                .filter(user -> user.username().equalsIgnoreCase(username))
                .findFirst()
                .map(user -> new UserCredential(user.id(), user.username(), user.passwordHash(), user.roles()));
    }
}
