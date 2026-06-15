package com.tyb.myblog.v2.content.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类持久化 Mapper，SQL 统一位于 XML。
 */
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity> {

    List<CategoryEntity> selectAllActive();

    CategoryEntity selectActiveById(@Param("id") long id);

    CategoryEntity selectActiveByIdForUpdate(@Param("id") long id);

    List<CategoryEntity> selectActiveByIdsForUpdate(
            @Param("ids") List<Long> ids);

    CategoryEntity selectBySlugIncludingDeleted(
            @Param("slug") String slug);
}
