package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.api.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void usesFirstForwardedForIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.20");
        request.addHeader("X-Real-IP", "198.51.100.30");
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void fallsBackToRealIpHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.30");
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.30");
    }

    @Test
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void returnsNullWhenAllValuesAreBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", " , ");
        request.addHeader("X-Real-IP", " ");
        request.setRemoteAddr(" ");

        assertThat(ClientIpResolver.resolve(request)).isNull();
    }
}
