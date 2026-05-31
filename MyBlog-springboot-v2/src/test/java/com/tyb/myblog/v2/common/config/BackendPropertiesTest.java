package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class BackendPropertiesTest {

    @Autowired
    private ApiCorsProperties corsProperties;

    @Autowired
    private SecurityPublicEndpointProperties securityProperties;

    @Test
    void bindsCorsOrigins() {
        assertThat(corsProperties.allowedOrigins()).containsExactly("http://localhost:5173");
    }

    @Test
    void bindsPublicEndpoints() {
        assertThat(securityProperties.publicEndpoints())
                .contains("/actuator/health", "/api/public/security-probe");
    }
}
