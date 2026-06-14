package com.tyb.myblog.v2.system.domain.siteconfig;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 全站唯一的站点配置，负责字段规范化、校验和多语言回退。
 */
public record SiteConfig(
        long id,
        String siteTitleZh,
        String siteTitleJa,
        String siteTitleEn,
        String siteSubtitleZh,
        String siteSubtitleJa,
        String siteSubtitleEn,
        String aboutMdZh,
        String aboutMdJa,
        String aboutMdEn,
        String logoUrl,
        String faviconUrl,
        String icpNo,
        String spotifyPlaylistId,
        LocalDateTime updatedAt,
        Long updatedBy
) {

    public static final long FIXED_ID = 1L;
    private static final int ABOUT_MARKDOWN_MAX_LENGTH = 50_000;
    private static final Pattern SPOTIFY_ID =
            Pattern.compile("^[A-Za-z0-9_-]+$");

    /**
     * 创建经过完整规范化和校验的站点配置。
     *
     * @return 可安全持久化的站点配置
     */
    public static SiteConfig create(
            long id,
            String siteTitleZh,
            String siteTitleJa,
            String siteTitleEn,
            String siteSubtitleZh,
            String siteSubtitleJa,
            String siteSubtitleEn,
            String aboutMdZh,
            String aboutMdJa,
            String aboutMdEn,
            String logoUrl,
            String faviconUrl,
            String icpNo,
            String spotifyPlaylistId,
            LocalDateTime updatedAt,
            Long updatedBy) {
        if (id != FIXED_ID) {
            throw new IllegalArgumentException("站点配置 ID 必须固定为1");
        }
        return new SiteConfig(
                id,
                required(siteTitleZh, "中文站点标题", 128),
                optional(siteTitleJa, "日文站点标题", 128),
                optional(siteTitleEn, "英文站点标题", 128),
                optional(siteSubtitleZh, "中文站点副标题", 255),
                optional(siteSubtitleJa, "日文站点副标题", 255),
                optional(siteSubtitleEn, "英文站点副标题", 255),
                markdown(aboutMdZh, "中文关于我"),
                markdown(aboutMdJa, "日文关于我"),
                markdown(aboutMdEn, "英文关于我"),
                url(logoUrl, "站点 Logo URL"),
                url(faviconUrl, "站点 favicon URL"),
                optional(icpNo, "ICP备案号", 64),
                spotifyId(spotifyPlaylistId),
                updatedAt,
                updatedBy);
    }

    /**
     * 获取指定语言的站点标题。
     */
    public String siteTitle(SiteLanguage language) {
        return localized(language, siteTitleZh, siteTitleJa, siteTitleEn);
    }

    /**
     * 获取指定语言的站点副标题。
     */
    public String siteSubtitle(SiteLanguage language) {
        return localized(
                language,
                siteSubtitleZh,
                siteSubtitleJa,
                siteSubtitleEn);
    }

    /**
     * 获取指定语言的关于我 Markdown。
     */
    public String aboutMd(SiteLanguage language) {
        return localized(language, aboutMdZh, aboutMdJa, aboutMdEn);
    }

    private static String localized(
            SiteLanguage language,
            String chinese,
            String japanese,
            String english) {
        return switch (Objects.requireNonNull(language, "语言不能为空")) {
            case ZH -> chinese;
            case JA -> japanese == null ? chinese : japanese;
            case EN -> english == null ? chinese : english;
        };
    }

    private static String required(
            String value,
            String field,
            int maxLength) {
        String normalized = optional(value, field, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return normalized;
    }

    private static String optional(
            String value,
            String field,
            int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    private static String markdown(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() > ABOUT_MARKDOWN_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    field + "不能超过" + ABOUT_MARKDOWN_MAX_LENGTH + "个字符");
        }
        return value;
    }

    private static String url(String value, String field) {
        String normalized = optional(value, field, 255);
        if (normalized == null) {
            return null;
        }

        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + "格式错误", exception);
        }
        if (uri.getHost() == null
                || (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException(
                    field + "仅支持绝对 HTTP 或 HTTPS URL");
        }
        return normalized;
    }

    private static String spotifyId(String value) {
        String normalized =
                optional(value, "Spotify 播放列表 ID", 64);
        if (normalized != null
                && !SPOTIFY_ID.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Spotify 播放列表 ID 格式错误");
        }
        return normalized;
    }
}
