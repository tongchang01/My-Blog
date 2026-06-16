package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.content.application.article.AdminArticleDetailResult;
import com.tyb.myblog.v2.content.application.article.AdminArticlePageResult;
import com.tyb.myblog.v2.content.application.article.DeletedArticlePageResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleDetailResult;
import com.tyb.myblog.v2.content.application.article.PublicArticlePageResult;
import com.tyb.myblog.v2.content.domain.article.AdminArticlePageItem;
import org.springframework.stereotype.Component;

/**
 * 文章应用结果到 HTTP 响应的显式映射。
 */
@Component
public class ArticleWebMapping {

    public PageResponse<AdminArticlePageItemVO> toAdminPage(
            AdminArticlePageResult result) {
        return new PageResponse<>(
                result.records().stream()
                        .map(this::toAdminPageItem)
                        .toList(),
                result.total(),
                result.page(),
                result.size());
    }

    public AdminArticleDetailVO toAdminDetail(
            AdminArticleDetailResult result) {
        return new AdminArticleDetailVO(
                result.id(),
                result.titleZh(),
                result.titleJa(),
                result.titleEn(),
                result.summaryZh(),
                result.summaryJa(),
                result.summaryEn(),
                result.body(),
                result.categoryId(),
                result.categoryNameZh(),
                result.authorId(),
                result.slug(),
                result.status(),
                result.publishAt(),
                result.coverAttachmentId(),
                result.coverUrl(),
                result.commentCount(),
                result.tagIds(),
                result.createdAt(),
                result.createdBy(),
                result.updatedAt(),
                result.updatedBy());
    }

    public PageResponse<PublicArticlePageItemVO> toPublicPage(
            PublicArticlePageResult result) {
        return new PageResponse<>(
                result.records().stream()
                        .map(this::toPublicPageItem)
                        .toList(),
                result.total(),
                result.page(),
                result.size());
    }

    public PublicArticleDetailVO toPublicDetail(
            PublicArticleDetailResult result) {
        return new PublicArticleDetailVO(
                result.id(),
                result.title(),
                result.summary(),
                result.body(),
                result.categoryId(),
                result.categoryName(),
                result.slug(),
                result.status(),
                result.publishAt(),
                result.coverAttachmentId(),
                result.coverUrl(),
                result.commentCount(),
                result.tags(),
                result.createdAt(),
                result.locked());
    }

    public PageResponse<DeletedArticlePageItemVO> toDeletedPage(
            DeletedArticlePageResult result) {
        return new PageResponse<>(
                result.records().stream()
                        .map(this::toDeletedPageItem)
                        .toList(),
                result.total(),
                result.page(),
                result.size());
    }

    private AdminArticlePageItemVO toAdminPageItem(
            AdminArticlePageItem item) {
        return new AdminArticlePageItemVO(
                item.id(),
                item.titleZh(),
                item.titleJa(),
                item.titleEn(),
                item.summaryZh(),
                item.summaryJa(),
                item.summaryEn(),
                item.categoryId(),
                item.categoryNameZh(),
                item.slug(),
                item.status(),
                item.publishAt(),
                item.coverAttachmentId(),
                item.coverUrl(),
                item.commentCount(),
                item.tagIds(),
                item.createdAt(),
                item.createdBy(),
                item.updatedAt(),
                item.updatedBy());
    }

    private PublicArticlePageItemVO toPublicPageItem(
            PublicArticlePageResult.Item item) {
        return new PublicArticlePageItemVO(
                item.id(),
                item.title(),
                item.summary(),
                item.categoryId(),
                item.categoryName(),
                item.slug(),
                item.status(),
                item.publishAt(),
                item.coverAttachmentId(),
                item.coverUrl(),
                item.commentCount(),
                item.tags(),
                item.createdAt(),
                item.locked());
    }

    private DeletedArticlePageItemVO toDeletedPageItem(
            DeletedArticlePageResult.Item item) {
        return new DeletedArticlePageItemVO(
                item.id(),
                item.titleZh(),
                item.titleJa(),
                item.titleEn(),
                item.status(),
                item.categoryId(),
                item.deletedAt(),
                item.deletedBy());
    }
}
