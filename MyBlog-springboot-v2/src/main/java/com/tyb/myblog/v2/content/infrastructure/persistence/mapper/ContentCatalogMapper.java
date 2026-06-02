package com.tyb.myblog.v2.content.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.content.domain.CategorySummary;
import com.tyb.myblog.v2.content.domain.TagSummary;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 内容目录 MyBatis-Plus Mapper。
 *
 * <p>基础泛型绑定 {@link CategoryEntity}，用于验证 Entity 和 Mapper 的放置方式。
 * 分类、标签文章数量统计属于跨表聚合查询，因此 SQL 统一放在 XML 中维护。</p>
 */
@Mapper
public interface ContentCatalogMapper extends BaseMapper<CategoryEntity> {

    /**
     * 查询分类及已发布文章数量。
     *
     * <p>旧库 {@code t_article.is_delete = 0} 且 {@code t_article.status = 1}
     * 表示文章可在前台公开展示。</p>
     */
    List<CategorySummary> listCategorySummaries();

    /**
     * 查询标签及已发布文章数量。
     */
    List<TagSummary> listTagSummaries();

    /**
     * 查询按文章数量倒序、标签 ID 正序排列的热门标签。
     */
    List<TagSummary> listTopTagSummaries(@Param("limit") int limit);
}
