package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeProfileConfigurationTest {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void usesLocalProfileByDefaultWhenNoProfileIsSpecified() throws Exception {
        PropertySource<?> application = load("application.yml");

        assertThat(application.getProperty("spring.profiles.default")).isEqualTo("local");
    }

    @Test
    void localProfileUsesV2DatabaseAndRequiresExplicitCredentials() throws Exception {
        PropertySource<?> local = load("application-local.yml");

        assertThat(local.getProperty("spring.datasource.url"))
                .asString()
                .contains("/myblog_v2_dev?")
                .startsWith("${MYBLOG_DATASOURCE_URL:");
        assertThat(local.getProperty("spring.datasource.username"))
                .isEqualTo("${MYBLOG_DATASOURCE_USERNAME}");
        assertThat(local.getProperty("spring.datasource.password"))
                .isEqualTo("${MYBLOG_DATASOURCE_PASSWORD}");
        assertThat(local.getProperty("myblog.security.jwt.secret"))
                .isEqualTo("${MYBLOG_JWT_SECRET}");
        assertThat(local.getProperty("myblog.stats.hash-secret"))
                .isEqualTo("${MYBLOG_STATS_HASH_SECRET}");
    }

    @Test
    void prodProfileRequiresExplicitSecretsAndKeepsDocumentationClosed() throws Exception {
        PropertySource<?> prod = load("application-prod.yml");

        assertThat(prod.getProperty("spring.datasource.url"))
                .isEqualTo("${MYBLOG_DATASOURCE_URL}");
        assertThat(prod.getProperty("spring.datasource.username"))
                .isEqualTo("${MYBLOG_DATASOURCE_USERNAME}");
        assertThat(prod.getProperty("spring.datasource.password"))
                .isEqualTo("${MYBLOG_DATASOURCE_PASSWORD}");
        assertThat(prod.getProperty("myblog.security.jwt.secret"))
                .isEqualTo("${MYBLOG_JWT_SECRET}");
        assertThat(prod.getProperty("myblog.stats.hash-secret"))
                .isEqualTo("${MYBLOG_STATS_HASH_SECRET}");
        assertThat(prod.getProperty("springdoc.api-docs.enabled")).isEqualTo(false);
        assertThat(prod.getProperty("springdoc.swagger-ui.enabled")).isEqualTo(false);
        assertThat(prod.getProperty("knife4j.enable")).isEqualTo(false);
        assertThat(prod.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health");
    }

    @Test
    void profilesDeclareOnlyAdditionalPublicEndpoints() throws Exception {
        PropertySource<?> application = load("application.yml");
        PropertySource<?> local = load("application-local.yml");
        PropertySource<?> test = load("application-test.yml");

        assertThat(application.getProperty(
                "myblog.security.public-endpoints[3].path"))
                .isEqualTo("/api/public/site-config");
        assertThat(application.getProperty(
                "myblog.security.public-endpoints[9].path"))
                .isEqualTo("/api/public/articles/*/comments");
        assertThat(application.getProperty(
                "myblog.security.public-endpoints[10].path"))
                .isEqualTo("/api/public/guestbook/comments");

        assertThat(local.getProperty(
                "myblog.security.public-endpoints[0].path")).isNull();
        assertThat(test.getProperty(
                "myblog.security.public-endpoints[0].path")).isNull();
        assertThat(local.getProperty(
                "myblog.security.additional-public-endpoints[0].path"))
                .isEqualTo("/api/public/security-probe");
        assertThat(test.getProperty(
                "myblog.security.additional-public-endpoints[5].path"))
                .isEqualTo("/media/**");
    }

    private PropertySource<?> load(String resourceName) throws Exception {
        List<PropertySource<?>> propertySources = loader.load(
                resourceName,
                new ClassPathResource(resourceName));
        assertThat(propertySources).hasSize(1);
        return propertySources.get(0);
    }
}
