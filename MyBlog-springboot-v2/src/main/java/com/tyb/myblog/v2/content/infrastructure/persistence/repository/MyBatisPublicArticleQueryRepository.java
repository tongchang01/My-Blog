package com.tyb.myblog.v2.content.infrastructure.persistence.repository;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.ArticleTagView;
import com.tyb.myblog.v2.content.domain.article.HomepageSlot;
import com.tyb.myblog.v2.content.domain.article.PublicArticleCriteria;
import com.tyb.myblog.v2.content.domain.article.PublicArticleAccessMetadata;
import com.tyb.myblog.v2.content.domain.article.PublicArticleDetail;
import com.tyb.myblog.v2.content.domain.article.PublicArticleHome;
import com.tyb.myblog.v2.content.domain.article.PublicArticlePage;
import com.tyb.myblog.v2.content.domain.article.PublicArticlePageItem;
import com.tyb.myblog.v2.content.domain.article.PublicArticleQueryRepository;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ArticleMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.ArticleTagRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.PublicArticleDetailRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.PublicArticlePageRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MyBatisPublicArticleQueryRepository
        implements PublicArticleQueryRepository {

    private final ArticleMapper mapper;

    @Override
    public PublicArticlePage findPublicPage(
            PublicArticleCriteria criteria) {
        long offset = (long) (criteria.page() - 1) * criteria.size();
        List<PublicArticlePageRow> rows = mapper.selectPublicPage(
                criteria,
                offset,
                criteria.size());
        Map<Long, List<ArticleTagView>> tags =
                tags(rows.stream().map(PublicArticlePageRow::getId).toList());
        return new PublicArticlePage(
                rows.stream()
                        .map(row -> toPageItem(
                                row,
                                tags.getOrDefault(
                                        row.getId(), List.of())))
                        .toList(),
                mapper.countPublicPage(criteria),
                criteria.page(),
                criteria.size());
    }

    @Override
    public PublicArticleHome findPublicHome(LocalDateTime now, int size) {
        List<PublicArticlePageRow> pinnedRows =
                mapper.selectPublicHomepageSlot(
                        HomepageSlot.PINNED,
                        now,
                        1);
        List<PublicArticlePageRow> featuredRows =
                mapper.selectPublicHomepageSlot(
                        HomepageSlot.FEATURED,
                        now,
                        2);
        List<PublicArticlePageRow> articleRows =
                mapper.selectPublicHomeArticles(now, size);
        List<PublicArticlePageRow> rows =
                java.util.stream.Stream.of(
                                pinnedRows,
                                featuredRows,
                                articleRows)
                        .flatMap(List::stream)
                        .toList();
        Map<Long, List<ArticleTagView>> tags =
                tags(rows.stream().map(PublicArticlePageRow::getId).toList());
        return new PublicArticleHome(
                pinnedRows.stream()
                        .findFirst()
                        .map(row -> toPageItem(
                                row,
                                tags.getOrDefault(row.getId(), List.of())))
                        .orElse(null),
                featuredRows.stream()
                        .map(row -> toPageItem(
                                row,
                                tags.getOrDefault(row.getId(), List.of())))
                        .toList(),
                articleRows.stream()
                        .map(row -> toPageItem(
                                row,
                                tags.getOrDefault(row.getId(), List.of())))
                        .toList());
    }

    @Override
    public Optional<PublicArticleAccessMetadata> findPublicAccessMetadata(
            long id,
            LocalDateTime now) {
        return Optional.ofNullable(
                        mapper.selectPublicAccessMetadata(id, now))
                .map(row -> new PublicArticleAccessMetadata(
                        row.getId(),
                        ArticleStatus.fromDatabase(row.getStatus())));
    }

    @Override
    public Optional<PublicArticleDetail> findPublicDetail(
            long id,
            LocalDateTime now) {
        return Optional.ofNullable(mapper.selectPublicDetail(id, now))
                .map(row -> toDetail(
                        row,
                        tags(List.of(row.getId()))
                                .getOrDefault(row.getId(), List.of())));
    }

    private Map<Long, List<ArticleTagView>> tags(List<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        return mapper.selectPublicArticleTags(articleIds).stream()
                .collect(Collectors.groupingBy(
                        ArticleTagRow::getArticleId,
                        Collectors.mapping(this::toTag, Collectors.toList())));
    }

    private PublicArticlePageItem toPageItem(
            PublicArticlePageRow row,
            List<ArticleTagView> tags) {
        return new PublicArticlePageItem(
                row.getId(),
                row.getTitleZh(),
                row.getTitleJa(),
                row.getTitleEn(),
                row.getSummaryZh(),
                row.getSummaryJa(),
                row.getSummaryEn(),
                row.getCategoryId(),
                row.getCategoryNameZh(),
                row.getCategoryNameJa(),
                row.getCategoryNameEn(),
                row.getSlug(),
                ArticleStatus.fromDatabase(row.getStatus()),
                row.getPublishAt(),
                row.getCoverAttachmentId(),
                null,
                row.getCommentCount(),
                tags,
                row.getCreatedAt());
    }

    private PublicArticleDetail toDetail(
            PublicArticleDetailRow row,
            List<ArticleTagView> tags) {
        return new PublicArticleDetail(
                row.getId(),
                row.getTitleZh(),
                row.getTitleJa(),
                row.getTitleEn(),
                row.getSummaryZh(),
                row.getSummaryJa(),
                row.getSummaryEn(),
                row.getBody(),
                row.getCategoryId(),
                row.getCategoryNameZh(),
                row.getCategoryNameJa(),
                row.getCategoryNameEn(),
                row.getSlug(),
                ArticleStatus.fromDatabase(row.getStatus()),
                row.getPublishAt(),
                row.getCoverAttachmentId(),
                null,
                row.getCommentCount(),
                tags,
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private ArticleTagView toTag(ArticleTagRow row) {
        return new ArticleTagView(
                row.getId(),
                row.getNameZh(),
                row.getNameJa(),
                row.getNameEn(),
                row.getSlug());
    }
}
