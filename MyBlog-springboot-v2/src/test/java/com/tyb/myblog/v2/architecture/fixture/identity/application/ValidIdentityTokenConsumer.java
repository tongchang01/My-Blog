package com.tyb.myblog.v2.architecture.fixture.identity.application;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;

public class ValidIdentityTokenConsumer {

    private final AccessTokenIssuer tokenIssuer;

    public ValidIdentityTokenConsumer(AccessTokenIssuer tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }
}
