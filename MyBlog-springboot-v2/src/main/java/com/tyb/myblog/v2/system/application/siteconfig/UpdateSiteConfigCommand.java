package com.tyb.myblog.v2.system.application.siteconfig;

import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfig;

import java.time.LocalDate;

/**
 * 站点配置全量更新命令。
 */
public record UpdateSiteConfigCommand(
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
        LocalDate startedDate
) {

    public UpdateSiteConfigCommand(
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
            String spotifyPlaylistId) {
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
                null);
    }

    /**
     * 使用当前配置的审计信息构建待持久化领域对象。
     */
    public SiteConfig toDomain(SiteConfig current) {
        return SiteConfig.create(
                SiteConfig.FIXED_ID,
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
                startedDate,
                current.updatedAt(),
                current.updatedBy());
    }
}
