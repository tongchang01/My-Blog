package com.tyb.myblog.v2.system.application.siteconfig;

import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 后台完整站点配置查询结果。
 */
public record AdminSiteConfigResult(
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
        LocalDate startedDate,
        LocalDateTime updatedAt,
        Long updatedBy
) {

    public AdminSiteConfigResult(
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
        this(
                siteTitleZh,
                siteTitleJa,
                siteTitleEn,
                siteSubtitleZh,
                siteSubtitleJa,
                siteSubtitleEn,
                aboutMdZh,
                aboutMdJa,
                aboutMdEn,
                logoUrl,
                faviconUrl,
                icpNo,
                spotifyPlaylistId,
                null,
                updatedAt,
                updatedBy);
    }

    /**
     * 从领域对象构建后台查询结果。
     */
    public static AdminSiteConfigResult from(SiteConfig config) {
        return new AdminSiteConfigResult(
                config.siteTitleZh(),
                config.siteTitleJa(),
                config.siteTitleEn(),
                config.siteSubtitleZh(),
                config.siteSubtitleJa(),
                config.siteSubtitleEn(),
                config.aboutMdZh(),
                config.aboutMdJa(),
                config.aboutMdEn(),
                config.logoUrl(),
                config.faviconUrl(),
                config.icpNo(),
                config.spotifyPlaylistId(),
                config.startedDate(),
                config.updatedAt(),
                config.updatedBy());
    }
}
