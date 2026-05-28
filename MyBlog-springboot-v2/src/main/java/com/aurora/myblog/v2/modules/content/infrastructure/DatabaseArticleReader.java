package com.aurora.myblog.v2.modules.content.infrastructure;

import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.content.domain.ArticlePageQuery;
import com.aurora.myblog.v2.modules.content.domain.ArticleReader;
import com.aurora.myblog.v2.modules.content.domain.ArticleSummary;
import com.aurora.myblog.v2.modules.content.domain.ArticleTagSummary;
import com.aurora.myblog.v2.modules.content.domain.AuthorSummary;
import com.aurora.myblog.v2.modules.content.domain.CategorySummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class DatabaseArticleReader implements ArticleReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseArticleReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PageResponse<ArticleSummary> listPublishedArticles(ArticlePageQuery query) {
        Long total = jdbcTemplate.queryForObject("""
                select count(*)
                from t_article a
                where a.is_delete = 0
                  and a.status = 1
                """, Long.class);
        List<Integer> ids = jdbcTemplate.queryForList("""
                select a.id
                from t_article a
                where a.is_delete = 0
                  and a.status = 1
                order by a.id desc
                limit ? offset ?
                """, Integer.class, query.size(), query.offset());
        return toPage(query, total, () -> loadArticleSummaries(ids));
    }

    @Override
    public PageResponse<ArticleSummary> listPublishedArticlesByCategory(int categoryId, ArticlePageQuery query) {
        Long total = jdbcTemplate.queryForObject("""
                select count(*)
                from t_article a
                where a.is_delete = 0
                  and a.status = 1
                  and a.category_id = ?
                """, Long.class, categoryId);
        List<Integer> ids = jdbcTemplate.queryForList("""
                select a.id
                from t_article a
                where a.is_delete = 0
                  and a.status = 1
                  and a.category_id = ?
                order by a.id desc
                limit ? offset ?
                """, Integer.class, categoryId, query.size(), query.offset());
        return toPage(query, total, () -> loadArticleSummaries(ids));
    }

    @Override
    public PageResponse<ArticleSummary> listPublishedArticlesByTag(int tagId, ArticlePageQuery query) {
        Long total = jdbcTemplate.queryForObject("""
                select count(distinct a.id)
                from t_article a
                join t_article_tag at on at.article_id = a.id
                where a.is_delete = 0
                  and a.status = 1
                  and at.tag_id = ?
                """, Long.class, tagId);
        List<Integer> ids = jdbcTemplate.queryForList("""
                select distinct a.id
                from t_article a
                join t_article_tag at on at.article_id = a.id
                where a.is_delete = 0
                  and a.status = 1
                  and at.tag_id = ?
                order by a.id desc
                limit ? offset ?
                """, Integer.class, tagId, query.size(), query.offset());
        return toPage(query, total, () -> loadArticleSummaries(ids));
    }

    private PageResponse<ArticleSummary> toPage(ArticlePageQuery query,
                                                Long total,
                                                Supplier<List<ArticleSummary>> recordsSupplier) {
        long safeTotal = total == null ? 0 : total;
        List<ArticleSummary> records = safeTotal == 0 ? List.of() : recordsSupplier.get();
        return new PageResponse<>(records, safeTotal, query.page(), query.size());
    }

    private List<ArticleSummary> loadArticleSummaries(List<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        List<ArticleSummaryRow> rows = jdbcTemplate.query("""
                        select a.id,
                               a.article_title,
                               a.article_abstract,
                               a.article_cover,
                               a.is_top,
                               a.is_featured,
                               a.create_time,
                               c.id as category_id,
                               c.category_name,
                               u.id as author_id,
                               u.nickname,
                               u.avatar,
                               t.id as tag_id,
                               t.tag_name
                        from t_article a
                        join t_category c on c.id = a.category_id
                        join t_user_info u on u.id = a.user_id
                        left join t_article_tag at on at.article_id = a.id
                        left join t_tag t on t.id = at.tag_id
                        where a.id in (%s)
                        order by a.id desc, t.id asc
                        """.formatted(placeholders),
                (rs, rowNum) -> new ArticleSummaryRow(
                        rs.getInt("id"),
                        rs.getString("article_title"),
                        rs.getString("article_abstract"),
                        rs.getString("article_cover"),
                        rs.getInt("is_top") == 1,
                        rs.getInt("is_featured") == 1,
                        toLocalDateTime(rs.getTimestamp("create_time")),
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getInt("author_id"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        (Integer) rs.getObject("tag_id"),
                        rs.getString("tag_name")),
                ids.toArray());
        return groupRows(rows);
    }

    private List<ArticleSummary> groupRows(List<ArticleSummaryRow> rows) {
        Map<Integer, ArticleAccumulator> articles = new LinkedHashMap<>();
        for (ArticleSummaryRow row : rows) {
            ArticleAccumulator article = articles.computeIfAbsent(row.id(), id -> new ArticleAccumulator(row));
            if (row.tagId() != null) {
                article.tags().add(new ArticleTagSummary(row.tagId(), row.tagName()));
            }
        }
        return articles.values().stream()
                .map(ArticleAccumulator::toSummary)
                .toList();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record ArticleSummaryRow(
            int id,
            String title,
            String summary,
            String cover,
            boolean top,
            boolean featured,
            LocalDateTime createdAt,
            int categoryId,
            String categoryName,
            int authorId,
            String nickname,
            String avatar,
            Integer tagId,
            String tagName
    ) {
    }

    private record ArticleAccumulator(ArticleSummaryRow row, List<ArticleTagSummary> tags) {

        ArticleAccumulator(ArticleSummaryRow row) {
            this(row, new ArrayList<>());
        }

        ArticleSummary toSummary() {
            return new ArticleSummary(
                    row.id(),
                    row.title(),
                    row.summary(),
                    row.cover(),
                    new CategorySummary(row.categoryId(), row.categoryName(), 0),
                    new AuthorSummary(row.authorId(), row.nickname(), row.avatar()),
                    tags,
                    row.top(),
                    row.featured(),
                    row.createdAt());
        }
    }
}
