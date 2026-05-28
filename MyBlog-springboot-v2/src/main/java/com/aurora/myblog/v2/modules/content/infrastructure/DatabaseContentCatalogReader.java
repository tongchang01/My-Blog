package com.aurora.myblog.v2.modules.content.infrastructure;

import com.aurora.myblog.v2.modules.content.domain.CategorySummary;
import com.aurora.myblog.v2.modules.content.domain.ContentCatalogReader;
import com.aurora.myblog.v2.modules.content.domain.TagSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseContentCatalogReader implements ContentCatalogReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseContentCatalogReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CategorySummary> listCategories() {
        return jdbcTemplate.query("""
                        select c.id,
                               c.category_name,
                               count(a.id) as article_count
                        from t_category c
                        left join t_article a
                               on a.category_id = c.id
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
