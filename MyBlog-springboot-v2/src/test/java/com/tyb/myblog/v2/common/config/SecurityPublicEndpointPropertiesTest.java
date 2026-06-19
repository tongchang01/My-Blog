package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPublicEndpointPropertiesTest {

    @Test
    void mergesBaseAndAdditionalEndpointsAndNormalizesMissingLists() {
        var base = new SecurityPublicEndpointProperties.PublicEndpoint(
                "GET", "/api/public/site-config");
        var additional = new SecurityPublicEndpointProperties.PublicEndpoint(
                "GET", "/api/public/security-probe");

        var properties = new SecurityPublicEndpointProperties(
                List.of(base),
                List.of(additional));

        assertThat(properties.allPublicEndpoints())
                .containsExactly(base, additional);
        assertThat(new SecurityPublicEndpointProperties(null, null)
                .allPublicEndpoints())
                .isEmpty();
    }
}
