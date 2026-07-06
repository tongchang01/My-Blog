package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
        assertThat(securityProperties.allPublicEndpoints())
                .extracting(
                        SecurityPublicEndpointProperties.PublicEndpoint::method,
                        SecurityPublicEndpointProperties.PublicEndpoint::path)
                .containsExactly(
                        tuple("GET", "/actuator/health"),
                        tuple("POST", "/api/auth/login"),
                        tuple("POST", "/api/auth/refresh"),
                        tuple("GET", "/api/public/site-config"),
                        tuple("GET", "/api/public/friend-links"),
                        tuple("GET", "/api/public/categories"),
                        tuple("GET", "/api/public/tags"),
                        tuple("GET", "/api/public/articles"),
                        tuple("GET", "/api/public/articles/**"),
                        tuple("GET", "/api/public/archives"),
                        tuple("POST", "/api/public/articles/*/comments"),
                        tuple("GET", "/api/public/guestbook/comments"),
                        tuple("POST", "/api/public/guestbook/comments"),
                        tuple("POST", "/api/public/stats/page-views"),
                        tuple("GET", "/api/public/stats/site-summary"),
                        tuple("GET", "/api/public/security-probe"),
                        tuple("GET", "/doc.html"),
                        tuple("GET", "/webjars/**"),
                        tuple("GET", "/v3/api-docs/**"),
                        tuple("GET", "/swagger-ui/**"),
                        tuple("GET", "/media/**"));
    }
}
