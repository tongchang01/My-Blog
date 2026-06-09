package com.tyb.myblog.v2.common.infrastructure.persistence;

import org.flywaydb.core.Flyway;
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
    void migratesFrozenV1OnMySql() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);

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
    }
}
