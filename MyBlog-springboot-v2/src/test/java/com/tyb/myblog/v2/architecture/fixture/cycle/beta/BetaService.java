package com.tyb.myblog.v2.architecture.fixture.cycle.beta;

import com.tyb.myblog.v2.architecture.fixture.cycle.alpha.AlphaService;

public class BetaService {

    private final AlphaService alphaService;

    public BetaService(AlphaService alphaService) {
        this.alphaService = alphaService;
    }
}
