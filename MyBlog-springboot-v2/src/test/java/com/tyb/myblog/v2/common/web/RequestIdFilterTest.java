package com.tyb.myblog.v2.common.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void generatesRequestIdWhenHeaderMissingAndClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] mdcDuringChain = new String[1];

        filter.doFilter(request, response, (req, res) ->
                mdcDuringChain[0] = MDC.get("requestId"));

        assertThat(mdcDuringChain[0]).isNotBlank();
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(mdcDuringChain[0]);
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void reusesSafeInboundRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "abc-123_DEF.45");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("abc-123_DEF.45");
    }

    @Test
    void replacesUnsafeInboundRequestIdToPreventLogInjection() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "bad id\nwith newline");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        String assigned = response.getHeader("X-Request-Id");
        assertThat(assigned).isNotEqualTo("bad id\nwith newline");
        assertThat(assigned).matches("[A-Za-z0-9-]{36}");
    }
}
