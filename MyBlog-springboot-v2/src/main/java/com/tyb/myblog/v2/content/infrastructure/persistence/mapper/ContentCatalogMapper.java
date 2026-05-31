package com.tyb.myblog.v2.content.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.content.domain.CategorySummary;
import com.tyb.myblog.v2.content.domain.TagSummary;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 内容目录 MyBatis-Plus Mapper。
 *
 * <p>基础泛型绑定 {@link CategoryEntity}，用于验证 Entity 和 Mapper 的放置方式。
 * 分类、标签文章数量统计属于跨表聚合查询，因此使用显式 SQL 注解保留旧库兼容条件。</p>
 */
@Mapper
public interface ContentCatalogMapper extends BaseMapper<CategoryEntity> {

    /**
     * 查询分类及已发布文章数量。
     *
     * <p>旧库 {@code t_article.is_delete = 0} 且 {@code t_article.status = 1}
     * 表示文章可在前台公开展示。</p>
     */
    @Select("""
            select c.id,
                   c.category_name as name,
                   count(a.id) as articleCount
            from t_category c
            left join t_article a
                   on a.category_id = c.id
                  and a.is_delete = 0
                  and a.status = 1
            group by c.id, c.category_name
            order by c.id asc
            """)
    List<CategorySummary> listCategorySummaries();

    /**
     * 查询标签及已发布文章数量。
     */
    @Select("""
            select t.id,
                   t.tag_name as name,
                   count(a.id) as articleCount
            from t_tag t
            left join t_article_tag at on at.tag_id = t.id
            left join t_article a
                   on a.id = at.article_id
                  and a.is_delete = 0
                  and a.status = 1
            group by t.id, t.tag_name
            order by t.id asc
            """)
    List<TagSummary> listTagSummaries();

    /**
     * 查询按文章数量倒序、标签 ID 正序排列的热门标签。
     */
    @Select("""
            select t.id,
                   t.tag_name as name,
                   count(a.id) as articleCount
            from t_tag t
            left join t_article_tag at on at.tag_id = t.id
            left join t_article a
                   on a.id = at.article_id
                  and a.is_delete = 0
                  and a.status = 1
            group by t.id, t.tag_name
            order by articleCount desc, t.id asc
            limit #{limit}
            """)
    List<TagSummary> listTopTagSummaries(@Param("limit") int limit);
}
