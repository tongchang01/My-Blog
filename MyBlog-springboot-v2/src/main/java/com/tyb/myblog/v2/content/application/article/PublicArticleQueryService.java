package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.ArticleTagView;
import com.tyb.myblog.v2.content.domain.article.PublicArticleAccessMetadata;
import com.tyb.myblog.v2.content.domain.article.PublicArticleDetail;
import com.tyb.myblog.v2.content.domain.article.PublicArticleHome;
import com.tyb.myblog.v2.content.domain.article.PublicArticlePage;
import com.tyb.myblog.v2.content.domain.article.PublicArticlePageItem;
import com.tyb.myblog.v2.content.domain.article.PublicArticleQueryRepository;
import com.tyb.myblog.v2.system.application.attachment.AttachmentReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private static final int MAX_HOME_SIZE = 50;

    private final PublicArticleQueryRepository repository;
    private final PublicArticleAccessService accessService;
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
                                item.coverAttachmentId() == null
                                        ? null
                                        : coverUrls.get(item.coverAttachmentId())))
                        .toList(),
                page.total(),
                page.page(),
                page.size());
    }

    public PublicArchivePageResult archives(PublicArticleQuery query) {
        validateQuery(query);
        PublicArticlePage page = repository.findPublicPage(
                query.toCriteria(LocalDateTime.now(clock)));
        String lang = normalizeLang(query.lang());
        Map<YearMonth, List<PublicArchivePageResult.Item>> groups =
                new LinkedHashMap<>();
        for (PublicArticlePageItem item : page.records()) {
            YearMonth yearMonth = YearMonth.from(item.publishAt());
            groups.computeIfAbsent(yearMonth, ignored -> new ArrayList<>())
                    .add(new PublicArchivePageResult.Item(
                            item.id(),
                            localized(
                                    lang,
                                    item.titleZh(),
                                    item.titleJa(),
                                    item.titleEn()),
                            item.slug(),
                            item.publishAt(),
                            localized(
                                    lang,
                                    item.summaryZh(),
                                    item.summaryJa(),
                                    item.summaryEn())));
        }
        return new PublicArchivePageResult(
                groups.entrySet().stream()
                        .map(entry -> new PublicArchivePageResult.Group(
                                entry.getKey().toString(),
                                entry.getKey().getYear(),
                                entry.getKey().getMonthValue(),
                                entry.getValue()))
                        .toList(),
                page.total(),
                page.page(),
                page.size());
    }

    public PublicArticleHomeResult home(String lang, int size) {
        if (size < 1 || size > MAX_HOME_SIZE) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "首页文章数量必须在 1 到 50 之间");
        }
        PublicArticleHome home = repository.findPublicHome(
                LocalDateTime.now(clock),
                size);
        Map<Long, String> coverUrls = resolveCoverUrls(home);
        String normalizedLang = normalizeLang(lang);
        return new PublicArticleHomeResult(
                home.pinnedArticle() == null
                        ? null
                        : toItem(home.pinnedArticle(), normalizedLang, coverUrls),
                home.featuredArticles().stream()
                        .map(item -> toItem(item, normalizedLang, coverUrls))
                        .toList(),
                home.articles().stream()
                        .map(item -> toItem(item, normalizedLang, coverUrls))
                        .toList());
    }

    public PublicArticleDetailResult detail(long id, String lang) {
        return detail(id, lang, null);
    }

    public PublicArticleDetailResult detail(
            long id,
            String lang,
            String articleAccessToken) {
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "文章 ID 必须为正数");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        PublicArticleAccessMetadata access = repository
                .findPublicAccessMetadata(id, now)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "文章不存在"));
        accessService.requireAccess(access, articleAccessToken, now);
        PublicArticleDetail detail = repository.findPublicDetail(id, now)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "文章不存在"));
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
                item.publishAt(),
                coverUrl,
                item.commentCount(),
                tags(item.tags(), lang),
                item.createdAt(),
                item.status() == ArticleStatus.PASSWORD);
    }

    private PublicArticlePageResult.Item toItem(
            PublicArticlePageItem item,
            String lang,
            Map<Long, String> coverUrls) {
        return toItem(
                item,
                lang,
                item.coverAttachmentId() == null
                        ? null
                        : coverUrls.get(item.coverAttachmentId()));
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
                detail.publishAt(),
                coverUrl,
                detail.commentCount(),
                tags(detail.tags(), lang),
                detail.createdAt(),
                detail.updatedAt(),
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

    private Map<Long, String> resolveCoverUrls(PublicArticleHome home) {
        Set<Long> ids = java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(home.pinnedArticle()),
                        java.util.stream.Stream.concat(
                                home.featuredArticles().stream(),
                                home.articles().stream()))
                .filter(Objects::nonNull)
                .map(PublicArticlePageItem::coverAttachmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        return attachmentService.resolvePublicUrls(ids);
    }
}
