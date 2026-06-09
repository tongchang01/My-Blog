package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDatasourceConfigurationTest {

    @Test
    void forcesMySqlSessionToAsiaTokyo() throws Exception {
        PropertySource<?> localProperties = new YamlPropertySourceLoader()
                .load("application-local", new ClassPathResource("application-local.yml"))
                .get(0);
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(localProperties);

        String datasourceUrl = environment.getProperty("spring.datasource.url");

        assertThat(datasourceUrl)
                .contains("connectionTimeZone=Asia/Tokyo")
                .contains("forceConnectionTimeToSession=true")
                .contains("sessionVariables=time_zone='%2B09:00'")
                .doesNotContain("serverTimezone=");
    }
}
