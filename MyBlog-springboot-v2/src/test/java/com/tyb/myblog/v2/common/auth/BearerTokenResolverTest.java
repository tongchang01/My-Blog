package com.tyb.myblog.v2.common.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BearerTokenResolverTest {

    private final BearerTokenResolver resolver = new BearerTokenResolver();

    @Test
    void resolvesTokenWhenAuthorizationHeaderUsesStandardBearerPrefix() {
        assertThat(resolver.resolve("Bearer access-token"))
                .contains("access-token");
    }

    @Test
    void rejectsAuthorizationHeaderWhenPrefixIsNotStandard() {
        assertThat(resolver.resolve("bearer access-token")).isEmpty();
        assertThat(resolver.resolve("Bearer  access-token")).isEmpty();
        assertThat(resolver.resolve(" Bearer access-token")).isEmpty();
    }

    @Test
    void rejectsAuthorizationHeaderWhenTokenIsMissing() {
        assertThat(resolver.resolve(null)).isEmpty();
        assertThat(resolver.resolve("")).isEmpty();
        assertThat(resolver.resolve("Basic access-token")).isEmpty();
        assertThat(resolver.resolve("Bearer ")).isEmpty();
    }
}
