package com.tyb.myblog.v2.content.infrastructure.persistence.repository;

import com.tyb.myblog.v2.content.domain.article.AdminArticleCriteria;
import com.tyb.myblog.v2.content.domain.article.AdminArticleDetail;
import com.tyb.myblog.v2.content.domain.article.AdminArticlePage;
import com.tyb.myblog.v2.content.domain.article.AdminArticlePageItem;
import com.tyb.myblog.v2.content.domain.article.AdminArticleQueryRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.DeletedArticlePage;
import com.tyb.myblog.v2.content.domain.article.DeletedArticlePageItem;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ArticleMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.AdminArticleDetailRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.AdminArticlePageRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.DeletedArticlePageRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis XML 的后台文章查询仓储。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisAdminArticleQueryRepository
        implements AdminArticleQueryRepository {

    private final ArticleMapper mapper;

    @Override
    public AdminArticlePage findActivePage(AdminArticleCriteria criteria) {
        long offset = (long) (criteria.page() - 1) * criteria.size();
        return new AdminArticlePage(
                mapper.selectAdminPage(criteria, offset, criteria.size())
                        .stream()
                        .map(this::toPageItem)
                        .toList(),
                mapper.countAdminPage(criteria),
                criteria.page(),
                criteria.size());
    }

    @Override
    public Optional<AdminArticleDetail> findActiveDetail(long id) {
        return Optional.ofNullable(mapper.selectAdminDetail(id))
                .map(this::toDetail);
    }

    @Override
    public DeletedArticlePage findDeletedPage(int page, int size) {
        long offset = (long) (page - 1) * size;
        return new DeletedArticlePage(
                mapper.selectDeletedPage(offset, size)
                        .stream()
                        .map(this::toDeletedItem)
                        .toList(),
                mapper.countDeletedPage(),
                page,
                size);
    }

    private AdminArticlePageItem toPageItem(
            AdminArticlePageRow row) {
        return new AdminArticlePageItem(
                row.getId(),
                row.getTitleZh(),
                row.getTitleJa(),
                row.getTitleEn(),
                row.getSummaryZh(),
                row.getSummaryJa(),
                row.getSummaryEn(),
                row.getCategoryId(),
                row.getCategoryNameZh(),
                row.getSlug(),
                ArticleStatus.fromDatabase(row.getStatus()),
                row.getPublishAt(),
                row.getCoverAttachmentId(),
                null,
                row.getCommentCount(),
                mapper.selectTagIds(row.getId()),
                row.getCreatedAt(),
                row.getCreatedBy(),
                row.getUpdatedAt(),
                row.getUpdatedBy());
    }

    private AdminArticleDetail toDetail(AdminArticleDetailRow row) {
        return new AdminArticleDetail(
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
                row.getAuthorId(),
                row.getSlug(),
                ArticleStatus.fromDatabase(row.getStatus()),
                row.getPublishAt(),
                row.getCoverAttachmentId(),
                null,
                row.getCommentCount(),
                mapper.selectTagIds(row.getId()),
                row.getCreatedAt(),
                row.getCreatedBy(),
                row.getUpdatedAt(),
                row.getUpdatedBy());
    }

    private DeletedArticlePageItem toDeletedItem(
            DeletedArticlePageRow row) {
        return new DeletedArticlePageItem(
                row.getId(),
                row.getTitleZh(),
                row.getTitleJa(),
                row.getTitleEn(),
                ArticleStatus.fromDatabase(row.getStatus()),
                row.getCategoryId(),
                row.getDeletedAt(),
                row.getDeletedBy());
    }
}
