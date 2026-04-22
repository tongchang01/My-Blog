package com.aurora.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "upload.s3")
public class s3ConfigProperties {

    private String url;

    private String region;

    private String accessKeyId;

    private String accessKeySecret;

    private String bucketName;

}
