package com.tyb.myblog.v2.common.storage.web;

import com.tyb.myblog.v2.common.storage.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageWebConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withBean(StorageProperties.class,
                            StorageProperties::new)
                    .withUserConfiguration(
                            LocalStorageWebConfiguration.class);

    @Test
    void doesNotRegisterLocalMediaMappingByDefaultForS3() {
        contextRunner
                .withPropertyValues("myblog.storage.type=S3")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(
                                LocalStorageWebConfiguration.class));
    }

    @Test
    void registersLocalMediaMappingByDefaultForLocal() {
        contextRunner
                .withPropertyValues("myblog.storage.type=LOCAL")
                .run(context -> assertThat(context)
                        .hasSingleBean(
                                LocalStorageWebConfiguration.class));
    }

    @Test
    void registersLocalMediaMappingWhenExplicitlyEnabledForS3() {
        contextRunner
                .withPropertyValues(
                        "myblog.storage.type=S3",
                        "myblog.storage.local.web-enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(
                                LocalStorageWebConfiguration.class));
    }

    @Test
    void doesNotRegisterLocalMediaMappingWhenExplicitlyDisabledForLocal() {
        contextRunner
                .withPropertyValues(
                        "myblog.storage.type=LOCAL",
                        "myblog.storage.local.web-enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(
                                LocalStorageWebConfiguration.class));
    }
}
