package com.tyb.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("myblog.security")
public record SecurityPublicEndpointProperties(List<String> publicEndpoints) {
}
