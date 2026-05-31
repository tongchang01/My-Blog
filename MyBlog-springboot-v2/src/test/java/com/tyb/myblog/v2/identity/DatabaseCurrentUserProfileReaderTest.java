package com.tyb.myblog.v2.identity;

import com.tyb.myblog.v2.identity.infrastructure.DatabaseCurrentUserProfileReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseCurrentUserProfileReader.class)
class DatabaseCurrentUserProfileReaderTest {

    @Autowired
    private DatabaseCurrentUserProfileReader reader;

    @Test
    void loadsProfileByAuthId() {
        var profile = reader.findByAuthId("1");

        assertThat(profile).isPresent();
        assertThat(profile.get().authId()).isEqualTo("1");
        assertThat(profile.get().userInfoId()).isEqualTo("1");
        assertThat(profile.get().username()).isEqualTo("admin@163.com");
        assertThat(profile.get().nickname()).isEqualTo("管理员");
        assertThat(profile.get().avatar()).isEqualTo("");
        assertThat(profile.get().email()).isEqualTo("admin@163.com");
    }

    @Test
    void returnsEmptyWhenAuthIdDoesNotExist() {
        assertThat(reader.findByAuthId("999")).isEmpty();
    }
}
