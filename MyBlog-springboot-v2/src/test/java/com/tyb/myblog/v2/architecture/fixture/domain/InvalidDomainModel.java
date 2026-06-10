package com.tyb.myblog.v2.architecture.fixture.domain;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.WebRequest;

public class InvalidDomainModel {

    private final HttpServletRequest request;
    private final WebRequest webRequest;
    private final BaseMapper<Object> mapper;

    public InvalidDomainModel(
            HttpServletRequest request,
            WebRequest webRequest,
            BaseMapper<Object> mapper) {
        this.request = request;
        this.webRequest = webRequest;
        this.mapper = mapper;
    }
}
