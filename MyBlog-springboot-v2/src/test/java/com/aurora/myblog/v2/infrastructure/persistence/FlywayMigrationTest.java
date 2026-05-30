package com.aurora.myblog.v2.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsV2SchemaMarkerFromMigration() {
        Integer markerCount = jdbcTemplate.queryForObject(
                "select count(*) from v2_schema_marker where marker_key = 'backend-v2-foundation'",
                Integer.class);

        assertThat(markerCount).isEqualTo(1);
    }

    @Test
    void migratesLegacyCommentTablesForTests() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from t_comment", Integer.class);

        assertThat(count).isNotNull();
        assertThat(count).isGreaterThanOrEqualTo(6);
    }
}
