package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.application.siteconfig.PublicSiteConfigResult;

import java.time.LocalDate;

/**
 * 前台当前语言站点配置响应。
 */
public record PublicSiteConfigVO(
        String siteTitle,
        String siteSubtitle,
        String aboutMd,
        String logoUrl,
        String faviconUrl,
        String icpNo,
        String spotifyPlaylistId,
        LocalDate startedDate
) {

    /**
     * 将应用查询结果转换为公开 HTTP 响应。
     */
    public static PublicSiteConfigVO from(
            PublicSiteConfigResult result) {
        return new PublicSiteConfigVO(
                result.siteTitle(),
                result.siteSubtitle(),
                result.aboutMd(),
                result.logoUrl(),
                result.faviconUrl(),
                result.icpNo(),
                result.spotifyPlaylistId(),
                result.startedDate());
    }
}
