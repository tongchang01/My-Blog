package com.tyb.myblog.v2.stats.infrastructure.persistence.repository;

import com.tyb.myblog.v2.stats.domain.PageViewEvent;
import com.tyb.myblog.v2.stats.domain.PageViewRepository;
import com.tyb.myblog.v2.stats.infrastructure.persistence.entity.PageViewEntity;
import com.tyb.myblog.v2.stats.infrastructure.persistence.mapper.PageViewMapper;
import com.tyb.myblog.v2.stats.infrastructure.persistence.mapping.PageViewPersistenceMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 基于 MyBatis-Plus 的访问明细仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisPageViewRepository implements PageViewRepository {

    private final PageViewMapper mapper;
    private final PageViewPersistenceMapping mapping;

    @Override
    public long append(PageViewEvent event) {
        PageViewEntity entity = mapping.toEntity(event);
        if (mapper.insert(entity) != 1
                || entity.getId() == null
                || entity.getId() <= 0) {
            throw new IllegalStateException("访问明细写入失败");
        }
        return entity.getId();
    }
}
