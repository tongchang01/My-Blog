package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.ArticleTagView;
import com.tyb.myblog.v2.content.domain.article.PublicArticleDetail;
import com.tyb.myblog.v2.content.domain.article.PublicArticlePage;
import com.tyb.myblog.v2.content.domain.article.PublicArticlePageItem;
import com.tyb.myblog.v2.content.domain.article.PublicArticleQueryRepository;
import com.tyb.myblog.v2.system.application.attachment.AttachmentReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicArticleQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PublicArticleQueryRepository repository;
    private final AttachmentReferenceService attachmentService;
    private final Clock clock;

    public PublicArticlePageResult page(PublicArticleQuery query) {
        validateQuery(query);
        LocalDateTime now = LocalDateTime.now(clock);
        PublicArticlePage page =
                repository.findPublicPage(query.toCriteria(now));
        Map<Long, String> coverUrls = resolveCoverUrls(page);
        String lang = normalizeLang(query.lang());
        return new PublicArticlePageResult(
                page.records().stream()
                        .map(item -> toItem(
                                item,
                                lang,
                                coverUrls.get(item.coverAttachmentId())))
                        .toList(),
                page.total(),
                page.page(),
                page.size());
    }

    public PublicArticleDetailResult detail(long id, String lang) {
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "文章 ID 必须为正数");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        PublicArticleDetail detail = repository.findPublicDetail(id, now)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "文章不存在"));
        if (detail.status() == ArticleStatus.PASSWORD) {
            throw new ApiException(
                    ApiErrorCode.FORBIDDEN,
                    "密码文章暂不开放正文访问");
        }
        String coverUrl = detail.coverAttachmentId() == null
                ? null
                : attachmentService.resolvePublicUrls(
                        Set.of(detail.coverAttachmentId()))
                .get(detail.coverAttachmentId());
        return toDetail(detail, normalizeLang(lang), coverUrl);
    }

    private void validateQuery(PublicArticleQuery query) {
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
        try {
            query.toCriteria(LocalDateTime.now(clock));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }
    }

    private PublicArticlePageResult.Item toItem(
            PublicArticlePageItem item,
            String lang,
            String coverUrl) {
        return new PublicArticlePageResult.Item(
                item.id(),
                localized(lang, item.titleZh(), item.titleJa(), item.titleEn()),
                localized(lang, item.summaryZh(), item.summaryJa(), item.summaryEn()),
                item.categoryId(),
                localized(
                        lang,
                        item.categoryNameZh(),
                        item.categoryNameJa(),
                        item.categoryNameEn()),
                item.slug(),
                item.status(),
                item.publishAt(),
                item.coverAttachmentId(),
                coverUrl,
                item.commentCount(),
                tags(item.tags(), lang),
                item.createdAt(),
                item.status() == ArticleStatus.PASSWORD);
    }

    private PublicArticleDetailResult toDetail(
            PublicArticleDetail detail,
            String lang,
            String coverUrl) {
        return new PublicArticleDetailResult(
                detail.id(),
                localized(lang, detail.titleZh(), detail.titleJa(), detail.titleEn()),
                localized(lang, detail.summaryZh(), detail.summaryJa(), detail.summaryEn()),
                detail.body(),
                detail.categoryId(),
                localized(
                        lang,
                        detail.categoryNameZh(),
                        detail.categoryNameJa(),
                        detail.categoryNameEn()),
                detail.slug(),
                detail.status(),
                detail.publishAt(),
                detail.coverAttachmentId(),
                coverUrl,
                detail.commentCount(),
                tags(detail.tags(), lang),
                detail.createdAt(),
                false);
    }

    private List<PublicArticleTagResult> tags(
            List<ArticleTagView> tags,
            String lang) {
        return tags.stream()
                .map(tag -> new PublicArticleTagResult(
                        tag.id(),
                        localized(lang, tag.nameZh(), tag.nameJa(), tag.nameEn()),
                        tag.slug()))
                .toList();
    }

    private String localized(
            String lang,
            String zh,
            String ja,
            String en) {
        String candidate = switch (lang) {
            case "ja" -> ja;
            case "en" -> en;
            default -> zh;
        };
        if (candidate != null && !candidate.isBlank()) {
            return candidate;
        }
        return zh;
    }

    private String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return "zh";
        }
        String normalized = lang.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ja", "en" -> normalized;
            default -> "zh";
        };
    }

    private Map<Long, String> resolveCoverUrls(PublicArticlePage page) {
        Set<Long> ids = page.records().stream()
                .map(PublicArticlePageItem::coverAttachmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        return attachmentService.resolvePublicUrls(ids);
    }
}
