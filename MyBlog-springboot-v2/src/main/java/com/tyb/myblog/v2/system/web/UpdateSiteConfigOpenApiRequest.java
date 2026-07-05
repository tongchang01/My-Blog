package com.tyb.myblog.v2.system.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 站点配置全量更新的 OpenAPI 文档模型。
 */
public record UpdateSiteConfigOpenApiRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String siteTitleZh,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String siteTitleJa,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String siteTitleEn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String siteSubtitleZh,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String siteSubtitleJa,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String siteSubtitleEn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String aboutMdZh,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String aboutMdJa,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String aboutMdEn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String logoUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String faviconUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String icpNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String spotifyPlaylistId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        LocalDate startedDate
) {
}
