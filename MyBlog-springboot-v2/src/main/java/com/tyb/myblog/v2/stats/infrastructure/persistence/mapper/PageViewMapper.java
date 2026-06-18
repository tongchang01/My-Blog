package com.tyb.myblog.v2.stats.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.stats.infrastructure.persistence.entity.PageViewEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 页面访问明细 Mapper，生产 SQL 统一位于 XML 或 BaseMapper。
 */
@Mapper
public interface PageViewMapper extends BaseMapper<PageViewEntity> {
}
