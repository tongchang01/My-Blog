package com.tyb.myblog.v2.identity.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前用户资料部分更新的 OpenAPI 文档模型。
 *
 * <p>该类型只描述客户端提交的 JSON 字符串或 null；运行时仍由
 * {@link UpdateCurrentUserProfileRequest} 保存字段是否出现的信息。</p>
 */
public record UpdateCurrentUserProfileOpenApiRequest(
        @Schema(types = {"string", "null"}) String nickname,
        @Schema(types = {"string", "null"}) String avatarUrl,
        @Schema(types = {"string", "null"}) String bioZh,
        @Schema(types = {"string", "null"}) String bioJa,
        @Schema(types = {"string", "null"}) String bioEn,
        @Schema(types = {"string", "null"}) String location,
        @Schema(types = {"string", "null"}) String website,
        @Schema(types = {"string", "null"}) String emailPublic,
        @Schema(types = {"string", "null"}) String githubUrl,
        @Schema(types = {"string", "null"}) String twitterUrl,
        @Schema(types = {"string", "null"}) String linkedinUrl,
        @Schema(types = {"string", "null"}) String zhihuUrl,
        @Schema(types = {"string", "null"}) String qiitaUrl,
        @Schema(types = {"string", "null"}) String juejinUrl
) {
}
