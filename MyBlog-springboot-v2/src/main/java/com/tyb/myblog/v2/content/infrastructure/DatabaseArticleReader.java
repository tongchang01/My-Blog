package com.tyb.myblog.v2.content.infrastructure;

import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.content.domain.ArticleAccessCheck;
import com.tyb.myblog.v2.content.domain.ArticleDetail;
import com.tyb.myblog.v2.content.domain.ArticlePageQuery;
import com.tyb.myblog.v2.content.domain.ArticleReader;
import com.tyb.myblog.v2.content.domain.ArticleSummary;
import com.tyb.myblog.v2.content.domain.ArticleTagSummary;
import com.tyb.myblog.v2.content.domain.ArchiveMonth;
import com.tyb.myblog.v2.content.domain.AuthorSummary;
import com.tyb.myblog.v2.content.domain.CategorySummary;
import com.tyb.myblog.v2.content.domain.FeaturedArticles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    @Override
    public FeaturedArticles findFeaturedArticles() {
        List<Integer> ids = jdbcTemplate.queryForList("""
                select a.id
                from t_article a
                where a.is_delete = 0
                  and a.status = 1
                  and (a.is_top = 1 or a.is_featured = 1)
                order by a.is_top desc, a.is_featured desc, a.id desc
                limit 3
                """, Integer.class);
        List<ArticleSummary> articles = loadArticleSummaries(ids);
        Optional<ArticleSummary> topArticle = articles.stream()
                .filter(ArticleSummary::top)
                .findFirst();
        List<ArticleSummary> featuredArticles = articles.stream()
                .filter(article -> topArticle.map(top -> top.id() != article.id()).orElse(true))
                .filter(ArticleSummary::featured)
                .limit(2)
                .toList();
        return new FeaturedArticles(topArticle, featuredArticles);
    }

    @Override
    public PageResponse<ArchiveMonth> listPublishedArchives(ArticlePageQuery query) {
        List<ArticleSummary> articles = loadArchiveArticles();
        Map<String, List<ArticleSummary>> articlesByMonth = articles.stream()
                .collect(Collectors.groupingBy(
                        this::toArchiveMonth,
                        LinkedHashMap::new,
                        Collectors.toList()));
        List<ArchiveMonth> allMonths = articlesByMonth.entrySet().stream()
                .map(entry -> new ArchiveMonth(entry.getKey(), entry.getValue()))
                .toList();
        int fromIndex = Math.min(query.offset(), allMonths.size());
        int toIndex = Math.min(fromIndex + query.size(), allMonths.size());
        return new PageResponse<>(allMonths.subList(fromIndex, toIndex), allMonths.size(), query.page(), query.size());
    }

    @Override
    public Optional<ArticleAccessCheck> findArticleAccessCheckById(int articleId) {
        List<ArticleAccessCheck> checks = jdbcTemplate.query("""
                        select a.id,
                               a.status,
                               a.is_delete,
                               a.password
                        from t_article a
                        where a.id = ?
                        """,
                (rs, rowNum) -> new ArticleAccessCheck(
                        rs.getInt("id"),
                        rs.getInt("status"),
                        rs.getInt("is_delete") == 1,
                        rs.getString("password")),
                articleId);
        return checks.stream().findFirst();
    }

    @Override
    public Optional<ArticleDetail> findPublishedArticleById(int articleId) {
        return findArticleByIdAndStatuses(articleId, List.of(1));
    }

    @Override
    public Optional<ArticleDetail> findAccessibleArticleById(int articleId) {
        return findArticleByIdAndStatuses(articleId, List.of(1, 2));
    }

    private Optional<ArticleDetail> findArticleByIdAndStatuses(int articleId, List<Integer> statuses) {
        String statusPlaceholders = String.join(",", statuses.stream().map(status -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(articleId);
        args.addAll(statuses);
        List<ArticleDetailRow> rows = jdbcTemplate.query("""
                        select a.id,
                               a.article_title,
                               a.article_abstract,
                               a.article_content,
                               a.article_cover,
                               a.is_top,
                               a.is_featured,
                               a.create_time,
                               a.update_time,
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
                        where a.id = ?
                          and a.is_delete = 0
                          and a.status in (%s)
                        order by t.id asc
                        """.formatted(statusPlaceholders),
                (rs, rowNum) -> new ArticleDetailRow(
                        rs.getInt("id"),
                        rs.getString("article_title"),
                        rs.getString("article_abstract"),
                        rs.getString("article_content"),
                        rs.getString("article_cover"),
                        rs.getInt("is_top") == 1,
                        rs.getInt("is_featured") == 1,
                        toLocalDateTime(rs.getTimestamp("create_time")),
                        toLocalDateTime(rs.getTimestamp("update_time")),
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getInt("author_id"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        (Integer) rs.getObject("tag_id"),
                        rs.getString("tag_name")),
                args.toArray());
        return toArticleDetail(rows);
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
        Map<Integer, ArticleSummary> articlesById = groupRows(rows).stream()
                .collect(Collectors.toMap(ArticleSummary::id, article -> article));
        return ids.stream()
                .map(articlesById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ArticleSummary> loadArchiveArticles() {
        List<Integer> ids = jdbcTemplate.queryForList("""
                select a.id
                from t_article a
                where a.is_delete = 0
                  and a.status = 1
                order by a.create_time desc, a.id desc
                """, Integer.class);
        return loadArticleSummaries(ids);
    }

    private String toArchiveMonth(ArticleSummary article) {
        return "%04d-%02d".formatted(article.createdAt().getYear(), article.createdAt().getMonthValue());
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

    private Optional<ArticleDetail> toArticleDetail(List<ArticleDetailRow> rows) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        ArticleDetailRow first = rows.get(0);
        List<ArticleTagSummary> tags = rows.stream()
                .filter(row -> row.tagId() != null)
                .map(row -> new ArticleTagSummary(row.tagId(), row.tagName()))
                .toList();
        return Optional.of(new ArticleDetail(
                first.id(),
                first.title(),
                first.summary(),
                first.content(),
                first.cover(),
                new CategorySummary(first.categoryId(), first.categoryName(), 0),
                new AuthorSummary(first.authorId(), first.nickname(), first.avatar()),
                tags,
                first.top(),
                first.featured(),
                first.createdAt(),
                first.updatedAt()));
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

    private record ArticleDetailRow(
            int id,
            String title,
            String summary,
            String content,
            String cover,
            boolean top,
            boolean featured,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
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
