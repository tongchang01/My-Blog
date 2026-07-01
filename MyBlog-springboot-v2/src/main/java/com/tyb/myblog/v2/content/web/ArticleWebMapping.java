package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.content.application.article.AdminArticleDetailResult;
import com.tyb.myblog.v2.content.application.article.AdminArticlePageResult;
import com.tyb.myblog.v2.content.application.article.DeletedArticlePageResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleDetailResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleHomeResult;
import com.tyb.myblog.v2.content.application.article.PublicArticlePageResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleTagResult;
import java.util.List;
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
                id(result.id()),
                result.titleZh(),
                result.titleJa(),
                result.titleEn(),
                result.summaryZh(),
                result.summaryJa(),
                result.summaryEn(),
                result.body(),
                nullableId(result.categoryId()),
                result.categoryNameZh(),
                id(result.authorId()),
                result.slug(),
                result.status(),
                result.publishAt(),
                nullableId(result.coverAttachmentId()),
                result.coverUrl(),
                result.commentCount(),
                ids(result.tagIds()),
                result.createdAt(),
                nullableId(result.createdBy()),
                result.updatedAt(),
                nullableId(result.updatedBy()));
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

    public PublicArticleHomeVO toPublicHome(PublicArticleHomeResult result) {
        return new PublicArticleHomeVO(
                result.pinnedArticle() == null
                        ? null
                        : toPublicPageItem(result.pinnedArticle()),
                result.featuredArticles().stream()
                        .map(this::toPublicPageItem)
                        .toList(),
                result.articles().stream()
                        .map(this::toPublicPageItem)
                        .toList());
    }

    public PublicArticleDetailVO toPublicDetail(
            PublicArticleDetailResult result) {
        return new PublicArticleDetailVO(
                id(result.id()),
                result.title(),
                result.summary(),
                result.body(),
                nullableId(result.categoryId()),
                result.categoryName(),
                result.slug(),
                result.publishAt(),
                result.coverUrl(),
                result.commentCount(),
                publicTags(result.tags()),
                result.createdAt(),
                result.updatedAt(),
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
            AdminArticlePageResult.Item item) {
        return new AdminArticlePageItemVO(
                id(item.id()),
                item.titleZh(),
                item.titleJa(),
                item.titleEn(),
                item.summaryZh(),
                item.summaryJa(),
                item.summaryEn(),
                nullableId(item.categoryId()),
                item.categoryNameZh(),
                item.slug(),
                item.status(),
                item.publishAt(),
                nullableId(item.coverAttachmentId()),
                item.coverUrl(),
                item.commentCount(),
                ids(item.tagIds()),
                item.createdAt(),
                nullableId(item.createdBy()),
                item.updatedAt(),
                nullableId(item.updatedBy()));
    }

    private PublicArticlePageItemVO toPublicPageItem(
            PublicArticlePageResult.Item item) {
        return new PublicArticlePageItemVO(
                id(item.id()),
                item.title(),
                item.summary(),
                nullableId(item.categoryId()),
                item.categoryName(),
                item.slug(),
                item.publishAt(),
                item.coverUrl(),
                item.commentCount(),
                publicTags(item.tags()),
                item.createdAt(),
                item.locked());
    }

    private String id(long value) {
        return Long.toString(value);
    }

    private String nullableId(Long value) {
        return value == null ? null : Long.toString(value);
    }

    private List<String> ids(List<Long> values) {
        return values.stream()
                .map(this::id)
                .toList();
    }

    private List<PublicArticleTagVO> publicTags(
            List<PublicArticleTagResult> values) {
        return values.stream()
                .map(tag -> new PublicArticleTagVO(
                        id(tag.id()), tag.name(), tag.slug()))
                .toList();
    }

    private DeletedArticlePageItemVO toDeletedPageItem(
            DeletedArticlePageResult.Item item) {
        return new DeletedArticlePageItemVO(
                id(item.id()),
                item.titleZh(),
                item.titleJa(),
                item.titleEn(),
                item.status(),
                nullableId(item.categoryId()),
                item.deletedAt(),
                nullableId(item.deletedBy()));
    }
}
