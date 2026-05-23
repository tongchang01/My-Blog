package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.infrastructure.ConfiguredUserCredentialReader;
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
