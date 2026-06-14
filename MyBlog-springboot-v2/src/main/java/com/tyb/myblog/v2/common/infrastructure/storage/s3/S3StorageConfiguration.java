package com.tyb.myblog.v2.common.infrastructure.storage.s3;

import com.tyb.myblog.v2.common.storage.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 客户端与附件存储装配。
 */
@Configuration
@ConditionalOnProperty(
        prefix = "myblog.storage.s3",
        name = {"region", "bucket", "public-base-url"})
public class S3StorageConfiguration {

    @Bean(destroyMethod = "close")
    S3Client s3Client(StorageProperties properties) {
        properties.validate();
        return S3Client.builder()
                .region(Region.of(properties.getS3().getRegion()))
                .credentialsProvider(
                        DefaultCredentialsProvider.builder().build())
                .build();
    }

    @Bean
    S3StorageService s3StorageService(
            S3Client client,
            StorageProperties properties) {
        return new S3StorageService(
                client,
                properties.getS3().getBucket(),
                properties.getS3().getPublicBaseUrl());
    }
}
