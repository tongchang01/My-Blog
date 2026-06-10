package com.tyb.myblog.v2.architecture.fixture.application;

import com.tyb.myblog.v2.architecture.fixture.infrastructure.persistence.mapper.InvalidMapper;
import com.tyb.myblog.v2.architecture.fixture.infrastructure.persistence.entity.InvalidEntity;
import com.tyb.myblog.v2.architecture.fixture.web.InvalidWebDto;

public class InvalidApplicationService {

    private final InvalidMapper mapper;
    private final InvalidEntity entity;
    private final InvalidWebDto dto;

    public InvalidApplicationService(InvalidMapper mapper, InvalidEntity entity, InvalidWebDto dto) {
        this.mapper = mapper;
        this.entity = entity;
        this.dto = dto;
    }
}
