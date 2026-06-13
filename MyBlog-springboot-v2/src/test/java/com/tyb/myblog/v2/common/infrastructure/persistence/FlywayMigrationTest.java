package com.tyb.myblog.v2.common.infrastructure.persistence;

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
    void createsAllV2SchemaTables() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.tables
                        where table_schema = 'PUBLIC'
                          and table_name in (
                              'T_USER_AUTH',
                              'T_USER_INFO',
                              'T_REFRESH_TOKEN',
                              'T_ARTICLE',
                              'T_ARTICLE_TAG',
                              'T_CATEGORY',
                              'T_TAG',
                              'T_COMMENT',
                              'T_SITE_CONFIG',
                              'T_ATTACHMENT',
                              'T_FRIEND_LINK',
                              'T_PAGE_VIEW',
                              'T_PAGE_VIEW_DAILY',
                              'T_MAIL_LOG'
                          )
                        """,
                Integer.class);

        assertThat(tableCount).isEqualTo(14);
    }

    @Test
    void insertsDefaultSiteConfigOnly() {
        Integer siteConfigCount = jdbcTemplate.queryForObject(
                "select count(*) from t_site_config where id = 1 and site_title_zh = 'MyBlog'",
                Integer.class);
        Integer userCount = jdbcTemplate.queryForObject("select count(*) from t_user_auth", Integer.class);

        assertThat(siteConfigCount).isEqualTo(1);
        assertThat(userCount).isZero();
    }

    @Test
    void appliesTwoMigrationsWithoutCreatingAccounts() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from "flyway_schema_history"
                        where "success" = true
                          and "version" is not null
                        """,
                Integer.class);
        String latestVersion = jdbcTemplate.queryForObject(
                "select max(\"version\") from \"flyway_schema_history\" where \"success\" = true",
                String.class);
        Integer userCount = jdbcTemplate.queryForObject(
                "select count(*) from t_user_auth",
                Integer.class);
        Integer profileCount = jdbcTemplate.queryForObject(
                "select count(*) from t_user_info",
                Integer.class);

        assertThat(migrationCount).isEqualTo(2);
        assertThat(latestVersion).isEqualTo("2");
        assertThat(userCount).isZero();
        assertThat(profileCount).isZero();
    }

    @Test
    void createsNewCommentSchemaColumns() {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where table_schema = 'PUBLIC'
                          and table_name = 'T_COMMENT'
                          and column_name in (
                              'TARGET_TYPE',
                              'TARGET_ID',
                              'CONTENT_MD',
                              'CONTENT_HTML',
                              'AUDIT_STATUS',
                              'DELETED',
                              'DELETED_AT',
                              'DELETED_BY'
                          )
                        """,
                Integer.class);

        assertThat(columnCount).isEqualTo(8);
    }
}
