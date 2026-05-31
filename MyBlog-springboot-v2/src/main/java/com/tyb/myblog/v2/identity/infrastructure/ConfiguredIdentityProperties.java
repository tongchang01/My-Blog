package com.tyb.myblog.v2.identity.infrastructure;

import com.tyb.myblog.v2.identity.domain.AuthRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("myblog.identity")
public record ConfiguredIdentityProperties(List<User> users) {

    public ConfiguredIdentityProperties {
        users = users == null ? List.of() : List.copyOf(users);
    }

    public record User(String id, String username, String passwordHash, List<AuthRole> roles) {
        public User {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
