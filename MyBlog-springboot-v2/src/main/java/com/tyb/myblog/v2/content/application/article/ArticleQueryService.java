package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.article.AdminArticleDetail;
import com.tyb.myblog.v2.content.domain.article.AdminArticleCriteria;
import com.tyb.myblog.v2.content.domain.article.AdminArticlePage;
import com.tyb.myblog.v2.content.domain.article.AdminArticlePageItem;
import com.tyb.myblog.v2.content.domain.article.AdminArticleQueryRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.system.application.attachment.AttachmentReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台文章查询用例。
 */
@Service
@RequiredArgsConstructor
public class ArticleQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminArticleQueryRepository repository;
    private final ContentAuthorization authorization;
    private final AttachmentReferenceService attachmentService;

    public AdminArticlePageResult adminPage(
            AuthenticatedPrincipal principal,
            AdminArticleQuery query) {
        authorization.requireReadable(principal);
        validateQuery(query);
        AdminArticlePage page = repository.findActivePage(toCriteria(query));
        Map<Long, String> coverUrls = resolveCoverUrls(page);
        return new AdminArticlePageResult(
                page.records().stream()
                        .map(item -> withCoverUrl(
                                item,
                                coverUrls.get(item.coverAttachmentId())))
                        .toList(),
                page.total(),
                page.page(),
                page.size());
    }

    public AdminArticleDetailResult adminDetail(
            AuthenticatedPrincipal principal,
            long id) {
        authorization.requireReadable(principal);
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "文章 ID 必须为正数");
        }
        AdminArticleDetail detail = repository.findActiveDetail(id)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "文章不存在"));
        String coverUrl = detail.coverAttachmentId() == null
                ? null
                : attachmentService.resolvePublicUrls(
                        Set.of(detail.coverAttachmentId()))
                .get(detail.coverAttachmentId());
        boolean includeBody = principal.roles().contains("ADMIN")
                || detail.status() == ArticleStatus.PUBLISHED;
        return AdminArticleDetailResult.from(
                detail,
                coverUrl,
                includeBody);
    }

    private void validateQuery(AdminArticleQuery query) {
        if (query.page() < 1) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "页码必须大于 0");
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "每页数量必须在 1 到 100 之间");
        }
        validateRange(query.createdFrom(), query.createdTo());
        validateRange(query.publishFrom(), query.publishTo());
    }

    private AdminArticleCriteria toCriteria(AdminArticleQuery query) {
        return new AdminArticleCriteria(
                query.page(),
                query.size(),
                query.status(),
                query.categoryId(),
                query.tagId(),
                query.titleKeyword(),
                query.createdFrom(),
                query.createdTo(),
                query.publishFrom(),
                query.publishTo());
    }

    private void validateRange(
            java.time.LocalDateTime from,
            java.time.LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "时间范围非法");
        }
    }

    private Map<Long, String> resolveCoverUrls(AdminArticlePage page) {
        Set<Long> ids = page.records().stream()
                .map(AdminArticlePageItem::coverAttachmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        return attachmentService.resolvePublicUrls(ids);
    }

    private AdminArticlePageResult.Item withCoverUrl(
            AdminArticlePageItem item,
            String coverUrl) {
        return new AdminArticlePageResult.Item(
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
                coverUrl,
                item.commentCount(),
                item.tagIds(),
                item.createdAt(),
                item.createdBy(),
                item.updatedAt(),
                item.updatedBy());
    }
}
