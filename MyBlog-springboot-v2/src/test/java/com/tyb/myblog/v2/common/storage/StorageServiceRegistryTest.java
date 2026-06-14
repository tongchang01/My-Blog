package com.tyb.myblog.v2.common.storage;

import com.tyb.myblog.v2.common.storage.config.StorageProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageServiceRegistryTest {

    @Test
    void routesCurrentAndRecordedStorageType() {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        StorageService local = service(StorageType.LOCAL);
        StorageService s3 = service(StorageType.S3);
        StorageServiceRegistry registry =
                new StorageServiceRegistry(properties, List.of(local, s3));

        assertThat(registry.current()).isSameAs(local);
        assertThat(registry.required(StorageType.S3)).isSameAs(s3);
    }

    @Test
    void rejectsMissingAndDuplicateStorageService() {
        StorageProperties properties = new StorageProperties();
        StorageService local = service(StorageType.LOCAL);

        assertThatThrownBy(() -> new StorageServiceRegistry(
                properties, List.of(local, local)))
                .isInstanceOf(IllegalStateException.class);
        StorageServiceRegistry registry =
                new StorageServiceRegistry(properties, List.of(local));
        assertThatThrownBy(() -> registry.required(StorageType.OSS))
                .isInstanceOf(StorageOperationException.class);
    }

    private StorageService service(StorageType type) {
        StorageService service = mock(StorageService.class);
        when(service.type()).thenReturn(type);
        return service;
    }
}
