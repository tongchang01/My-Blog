package com.tyb.myblog.v2.content.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.CategoryEntity;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.CategoryArticleCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类持久化 Mapper，SQL 统一位于 XML。
 */
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity> {

    List<CategoryEntity> selectAllActive();

    List<CategoryArticleCountRow> selectPublicWithArticleCount(
            @Param("now") LocalDateTime now);

    CategoryEntity selectActiveById(@Param("id") long id);

    CategoryEntity selectActiveByIdForUpdate(@Param("id") long id);

    List<CategoryEntity> selectActiveByIdsForUpdate(
            @Param("ids") List<Long> ids);

    CategoryEntity selectBySlugIncludingDeleted(
            @Param("slug") String slug);

    int updateActive(
            @Param("category") CategoryEntity category,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);

    int updateSortOrder(
            @Param("id") long id,
            @Param("sortOrder") int sortOrder,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);

    boolean existsActiveArticleReference(
            @Param("categoryId") long categoryId);

    int softDelete(
            @Param("id") long id,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deletedBy") long deletedBy);
}
