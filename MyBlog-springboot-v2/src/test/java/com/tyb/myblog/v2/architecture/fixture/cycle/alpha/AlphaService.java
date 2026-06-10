package com.tyb.myblog.v2.architecture.fixture.cycle.alpha;

import com.tyb.myblog.v2.architecture.fixture.cycle.beta.BetaService;

public class AlphaService {

    private final BetaService betaService;

    public AlphaService(BetaService betaService) {
        this.betaService = betaService;
    }
}
