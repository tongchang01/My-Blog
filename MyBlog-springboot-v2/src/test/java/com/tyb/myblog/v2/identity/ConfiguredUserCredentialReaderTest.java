package com.tyb.myblog.v2.identity;

import com.tyb.myblog.v2.identity.domain.AuthRole;
import com.tyb.myblog.v2.identity.infrastructure.ConfiguredUserCredentialReader;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguredUserCredentialReaderTest {

    @Test
    void loadsConfiguredUserWithoutExposingPasswordInPrincipal() {
        String hash = new BCryptPasswordEncoder().encode("correct-password");
        ConfiguredUserCredentialReader reader = ConfiguredUserCredentialReader.singleUser(
                "admin@example.com",
                hash,
                List.of(AuthRole.ADMIN));

        var credential = reader.findByUsername("admin@example.com");

        assertThat(credential).isPresent();
        assertThat(credential.get().username()).isEqualTo("admin@example.com");
        assertThat(credential.get().roles()).containsExactly(AuthRole.ADMIN);
        assertThat(credential.get().passwordHash()).isEqualTo(hash);
    }
}
