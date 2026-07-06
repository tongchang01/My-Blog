package com.tyb.myblog.v2.system.application.siteconfig;

import java.time.LocalDate;

/**
 * 前台当前语言站点配置查询结果。
 */
public record PublicSiteConfigResult(
        String siteTitle,
        String siteSubtitle,
        String aboutMd,
        String logoUrl,
        String faviconUrl,
        String icpNo,
        String spotifyPlaylistId,
        LocalDate startedDate
) {
}
