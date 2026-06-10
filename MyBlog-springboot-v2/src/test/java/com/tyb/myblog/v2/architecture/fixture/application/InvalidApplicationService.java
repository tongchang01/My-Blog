package com.tyb.myblog.v2.architecture.fixture.application;

import com.tyb.myblog.v2.architecture.fixture.infrastructure.persistence.mapper.InvalidMapper;

public class InvalidApplicationService {

    private final InvalidMapper mapper;

    public InvalidApplicationService(InvalidMapper mapper) {
        this.mapper = mapper;
    }
}
