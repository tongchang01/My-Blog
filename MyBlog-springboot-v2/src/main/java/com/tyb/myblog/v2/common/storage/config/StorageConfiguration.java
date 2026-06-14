package com.tyb.myblog.v2.common.storage.config;

import com.tyb.myblog.v2.common.infrastructure.storage.local.LocalStorageService;
import com.tyb.myblog.v2.common.storage.StorageService;
import com.tyb.myblog.v2.common.storage.StorageServiceRegistry;
import com.tyb.myblog.v2.common.storage.image.ImageInspector;
import com.tyb.myblog.v2.common.storage.image.UploadSpooler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;

/**
 * 附件存储公共组件装配。
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    @Bean
    UploadSpooler uploadSpooler() {
        return new UploadSpooler(Path.of(
                System.getProperty("java.io.tmpdir")));
    }

    @Bean
    ImageInspector imageInspector() {
        return new ImageInspector();
    }

    @Bean
    LocalStorageService localStorageService(StorageProperties properties) {
        properties.validate();
        return new LocalStorageService(
                properties.getLocal().getRoot(),
                properties.getLocal().getBucketAlias(),
                properties.getLocal().getPublicBaseUrl());
    }

    @Bean
    StorageServiceRegistry storageServiceRegistry(
            StorageProperties properties,
            List<StorageService> services) {
        return new StorageServiceRegistry(properties, services);
    }
}
