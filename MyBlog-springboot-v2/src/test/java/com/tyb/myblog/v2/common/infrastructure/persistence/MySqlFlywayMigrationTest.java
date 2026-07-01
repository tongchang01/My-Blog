package com.tyb.myblog.v2.common.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class MySqlFlywayMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("myblog_v2_test")
            .withUsername("myblog")
            .withPassword("myblog-test-password");

    @Test
    void migratesFrozenSchemaAndBackfillsUserProfileOnMySql() throws Exception {
        Flyway v1Flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("1"))
                .load();

        assertThat(v1Flyway.migrate().migrationsExecuted).isEqualTo(1);

        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.prepareStatement(
                     """
                             select count(*)
                             from information_schema.tables
                             where table_schema = ?
                               and table_name in (
                                   't_user_auth',
                                   't_user_info',
                                   't_refresh_token',
                                   't_article',
                                   't_article_tag',
                                   't_category',
                                   't_tag',
                                   't_comment',
                                   't_site_config',
                                   't_attachment',
                                   't_friend_link',
                                   't_page_view',
                                   't_page_view_daily',
                                   't_mail_log'
                               )
                             """)) {
            statement.setString(1, MYSQL.getDatabaseName());
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(14);
            }
        }

        insertAccountBeforeBackfill();

        Flyway latestFlyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load();

        assertThat(latestFlyway.migrate().migrationsExecuted).isEqualTo(2);
        assertMigrationCount(3);
        assertBackfilledProfile();
        assertThat(latestFlyway.migrate().migrationsExecuted).isZero();
        assertMigrationCount(3);
        assertBackfilledProfile();
    }

    private void insertAccountBeforeBackfill() throws Exception {
        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.prepareStatement(
                     """
                             INSERT INTO t_user_auth (
                                 id, username, password_hash, type, deleted
                             ) VALUES (?, ?, ?, ?, ?)
                             """)) {
            statement.setLong(1, 1001L);
            statement.setString(2, "admin");
            statement.setString(3, "$2a$10$test-password-hash");
            statement.setInt(4, 1);
            statement.setInt(5, 0);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private void assertBackfilledProfile() throws Exception {
        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.prepareStatement(
                     """
                             SELECT nickname, created_by, updated_by, deleted
                             FROM t_user_info
                             WHERE user_id = ?
                             """)) {
            statement.setLong(1, 1001L);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("nickname")).isEqualTo("admin");
                assertThat(result.getLong("created_by")).isEqualTo(1001L);
                assertThat(result.getLong("updated_by")).isEqualTo(1001L);
                assertThat(result.getInt("deleted")).isZero();
                assertThat(result.next()).isFalse();
            }
        }
    }

    private void assertMigrationCount(int expectedCount) throws Exception {
        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.prepareStatement(
                     """
                             SELECT COUNT(*)
                             FROM flyway_schema_history
                             WHERE success = 1
                               AND version IS NOT NULL
                             """);
             var result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(expectedCount);
        }
    }
}
