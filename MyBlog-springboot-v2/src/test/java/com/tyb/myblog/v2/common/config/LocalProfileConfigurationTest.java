package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class LocalProfileConfigurationTest {

    @Test
    void allowsLocalhostAndLoopbackForBothFrontendApplications() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application-local.yml"));
        Properties properties = loader.getObject();

        assertThat(properties)
                .containsEntry("myblog.cors.allowed-origins[0]", "http://localhost:5173")
                .containsEntry("myblog.cors.allowed-origins[1]", "http://127.0.0.1:5173")
                .containsEntry("myblog.cors.allowed-origins[2]", "http://localhost:8848")
                .containsEntry("myblog.cors.allowed-origins[3]", "http://127.0.0.1:8848");
    }
}
