package com.tyb.myblog.v2.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class UserAgentResolverTest {

    @Test
    void returnsNullWhenUserAgentIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", " ");

        assertThat(UserAgentResolver.resolve(request)).isNull();
    }

    @Test
    void truncatesLongUserAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "a".repeat(300));

        assertThat(UserAgentResolver.resolve(request)).hasSize(255);
    }
}
