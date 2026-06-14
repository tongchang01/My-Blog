package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.application.siteconfig.AdminSiteConfigResult;

import java.time.LocalDateTime;

/**
 * 后台完整站点配置响应。
 */
public record AdminSiteConfigVO(
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

    /**
     * 从应用层结果构建响应。
     */
    public static AdminSiteConfigVO from(AdminSiteConfigResult result) {
        return new AdminSiteConfigVO(
                result.siteTitleZh(),
                result.siteTitleJa(),
                result.siteTitleEn(),
                result.siteSubtitleZh(),
                result.siteSubtitleJa(),
                result.siteSubtitleEn(),
                result.aboutMdZh(),
                result.aboutMdJa(),
                result.aboutMdEn(),
                result.logoUrl(),
                result.faviconUrl(),
                result.icpNo(),
                result.spotifyPlaylistId(),
                result.updatedAt(),
                result.updatedBy());
    }
}
