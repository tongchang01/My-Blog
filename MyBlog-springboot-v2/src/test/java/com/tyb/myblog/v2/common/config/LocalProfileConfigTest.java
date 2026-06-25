package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class LocalProfileConfigTest {

    @Test
    void usesMySqlFlywayAndAsiaTokyoForLocalDevelopment() throws Exception {
        PropertySource<?> properties = new YamlPropertySourceLoader()
                .load("application-local", new ClassPathResource("application-local.yml"))
                .get(0);

        assertThat(properties.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(properties.getProperty("spring.flyway.enabled"))
                .isEqualTo(true);
        assertThat(properties.getProperty("spring.datasource.url"))
                .asString()
                .contains(
                        "myblog_v2_dev",
                        "allowPublicKeyRetrieval=true",
                        "Asia/Tokyo",
                        "time_zone='%2B09:00'");
    }
}
