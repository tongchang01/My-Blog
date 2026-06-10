package com.tyb.myblog.v2.common.web;

import com.tyb.myblog.v2.common.config.TrustedProxyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void ignoresForwardedHeadersForDirectRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.20");
        request.addHeader("X-Real-IP", "198.51.100.30");
        request.setRemoteAddr("198.51.100.40");

        assertThat(resolver().resolve(request)).isEqualTo("198.51.100.40");
    }

    @Test
    void usesFirstForwardedForIpFromTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.20");
        request.addHeader("X-Real-IP", "198.51.100.30");
        request.setRemoteAddr("10.0.0.8");

        assertThat(resolver("10.0.0.0/8").resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void fallsBackToRealIpHeaderForTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.30");
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver("127.0.0.1").resolve(request)).isEqualTo("198.51.100.30");
    }

    @Test
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver().resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void returnsNullWhenAllValuesAreBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", " , ");
        request.addHeader("X-Real-IP", " ");
        request.setRemoteAddr(" ");

        assertThat(resolver("127.0.0.1").resolve(request)).isNull();
    }

    private ClientIpResolver resolver(String... trustedProxies) {
        return new ClientIpResolver(new TrustedProxyProperties(List.of(trustedProxies)));
    }
}
