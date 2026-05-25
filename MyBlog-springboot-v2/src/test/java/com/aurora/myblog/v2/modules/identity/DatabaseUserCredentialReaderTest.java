package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.infrastructure.DatabaseUserCredentialReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseUserCredentialReader.class)
class DatabaseUserCredentialReaderTest {

    @Autowired
    private DatabaseUserCredentialReader reader;

    @Test
    void loadsActiveUserWithMappedRoles() {
        var credential = reader.findByUsername("admin@163.com");

        assertThat(credential).isPresent();
        assertThat(credential.get().id()).isEqualTo("1");
        assertThat(credential.get().username()).isEqualTo("admin@163.com");
        assertThat(credential.get().passwordHash()).startsWith("$2a$10$");
        assertThat(credential.get().roles()).containsExactly(AuthRole.ADMIN);
    }

    @Test
    void ignoresCaseWhenFindingUsername() {
        var credential = reader.findByUsername("ADMIN@163.COM");

        assertThat(credential).isPresent();
        assertThat(credential.get().username()).isEqualTo("admin@163.com");
    }

    @Test
    void doesNotLoadDisabledUser() {
        assertThat(reader.findByUsername("disabled@163.com")).isEmpty();
    }

    @Test
    void returnsEmptyWhenUserDoesNotExist() {
        assertThat(reader.findByUsername("missing@163.com")).isEmpty();
    }
}
