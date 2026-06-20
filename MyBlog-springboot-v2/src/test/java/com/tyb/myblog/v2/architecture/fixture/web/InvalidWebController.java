package com.tyb.myblog.v2.architecture.fixture.web;

import com.tyb.myblog.v2.architecture.fixture.domain.InvalidDomainDto;
import com.tyb.myblog.v2.architecture.fixture.infrastructure.persistence.entity.InvalidEntity;

public class InvalidWebController {

    private final InvalidEntity entity;

    public InvalidWebController(InvalidEntity entity) {
        this.entity = entity;
    }

    public InvalidDomainDto expose(InvalidDomainDto value) {
        return value;
    }
}
