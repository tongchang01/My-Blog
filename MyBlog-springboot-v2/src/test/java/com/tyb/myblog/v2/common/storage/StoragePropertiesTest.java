package com.tyb.myblog.v2.common.storage;

import com.tyb.myblog.v2.common.storage.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoragePropertiesTest {

    @Test
    void validatesAndNormalizesLocalConfiguration() {
        StorageProperties properties = bind(
                "myblog.storage.type=LOCAL",
                "myblog.storage.max-file-size=10MB",
                "myblog.storage.local.root=build/uploads",
                "myblog.storage.local.bucket-alias=local",
                "myblog.storage.local.public-base-url=http://localhost:8080/media/");

        properties.validate();

        assertThat(properties.getMaxFileBytes())
                .isEqualTo(10L * 1024 * 1024);
        assertThat(properties.getLocal().getPublicBaseUrl().toString())
                .isEqualTo("http://localhost:8080/media");
    }

    @Test
    void rejectsIncompleteCurrentBackendAndUnsupportedOssUpload() {
        StorageProperties incomplete = bind(
                "myblog.storage.type=LOCAL");
        StorageProperties oss = bind(
                "myblog.storage.type=OSS");

        assertThatThrownBy(incomplete::validate)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(oss::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OSS");
    }

    private StorageProperties bind(String... values) {
        MockEnvironment environment = new MockEnvironment();
        for (String value : values) {
            int separator = value.indexOf('=');
            environment.setProperty(
                    value.substring(0, separator),
                    value.substring(separator + 1));
        }
        return Binder.get(environment)
                .bind("myblog.storage", Bindable.of(StorageProperties.class))
                .orElseGet(StorageProperties::new);
    }
}
