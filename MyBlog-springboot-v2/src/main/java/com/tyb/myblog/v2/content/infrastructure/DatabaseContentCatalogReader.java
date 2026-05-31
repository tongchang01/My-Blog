package com.tyb.myblog.v2.content.infrastructure;

import com.tyb.myblog.v2.content.domain.CategorySummary;
import com.tyb.myblog.v2.content.domain.ContentCatalogReader;
import com.tyb.myblog.v2.content.domain.TagSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
/**
 * 基于旧库分类和标签表的内容目录读取器。
 *
 * <p>只统计未删除且已发布的文章，避免前台分类、标签数量包含草稿或软删除文章。</p>
 */
public class DatabaseContentCatalogReader implements ContentCatalogReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseContentCatalogReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询分类及已发布文章数量。
     */
    @Override
    public List<CategorySummary> listCategories() {
        return jdbcTemplate.query("""
                        select c.id,
                               c.category_name,
                               count(a.id) as article_count
                        from t_category c
                        left join t_article a
                               on a.category_id = c.id
                              -- 旧库 is_delete = 0 且 status = 1 表示前台可公开展示。
                              and a.is_delete = 0
                              and a.status = 1
                        group by c.id, c.category_name
                        order by c.id asc
                        """,
                (rs, rowNum) -> new CategorySummary(
                        rs.getInt("id"),
                        rs.getString("category_name"),
                        rs.getLong("article_count")));
    }

    /**
     * 查询标签及已发布文章数量。
     */
    @Override
    public List<TagSummary> listTags() {
        return jdbcTemplate.query("""
                        select t.id,
                               t.tag_name,
                               count(a.id) as article_count
                        from t_tag t
                        left join t_article_tag at on at.tag_id = t.id
                        left join t_article a
                               on a.id = at.article_id
                              and a.is_delete = 0
                              and a.status = 1
                        group by t.id, t.tag_name
                        order by t.id asc
                        """,
                (rs, rowNum) -> new TagSummary(
                        rs.getInt("id"),
                        rs.getString("tag_name"),
                        rs.getLong("article_count")));
    }

    /**
     * 查询按文章数量排序的热门标签。
     */
    @Override
    public List<TagSummary> listTopTags(int limit) {
        return jdbcTemplate.query("""
                        select t.id,
                               t.tag_name,
                               count(a.id) as article_count
                        from t_tag t
                        left join t_article_tag at on at.tag_id = t.id
                        left join t_article a
                               on a.id = at.article_id
                              and a.is_delete = 0
                              and a.status = 1
                        group by t.id, t.tag_name
                        order by article_count desc, t.id asc
                        limit ?
                        """,
                (rs, rowNum) -> new TagSummary(
                        rs.getInt("id"),
                        rs.getString("tag_name"),
                        rs.getLong("article_count")),
                Math.max(1, limit));
    }
}
