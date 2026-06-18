package com.tyb.myblog.v2.stats.infrastructure.persistence.mapping;

import com.tyb.myblog.v2.stats.domain.PageViewEvent;
import com.tyb.myblog.v2.stats.infrastructure.persistence.entity.PageViewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 页面访问领域对象与持久化实体的机械映射。
 */
@Mapper(componentModel = "spring")
public interface PageViewPersistenceMapping {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lang", expression = "java(event.language().code())")
    PageViewEntity toEntity(PageViewEvent event);
}
