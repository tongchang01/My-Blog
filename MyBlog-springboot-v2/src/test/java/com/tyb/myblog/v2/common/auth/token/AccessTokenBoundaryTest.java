package com.tyb.myblog.v2.common.auth.token;

import com.tyb.myblog.v2.common.security.auth.JwtTokenService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenBoundaryTest {

    @Test
    void jwtImplementationExposesStableCommonAuthPorts() {
        assertThat(AccessTokenIssuer.class).isAssignableFrom(JwtTokenService.class);
        assertThat(AccessTokenDecoder.class).isAssignableFrom(JwtTokenService.class);
        assertThat(AccessTokenVerifier.class.isAssignableFrom(JwtTokenService.class)).isFalse();
    }
}
