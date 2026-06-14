package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 友链完整写请求的 OpenAPI 模型。
 */
public record FriendLinkWriteOpenApiRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String url,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String avatarUrl,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Integer sortOrder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        FriendLinkStatus status
) {
}
