package com.tyb.myblog.v2.content.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 后台分类响应。
 */
public record AdminCategoryVO(
        @Schema(format = "int64") String id,
        String nameZh,
        String nameJa,
        String nameEn,
        String slug,
        int sortOrder,
        LocalDateTime createdAt,
        @Schema(format = "int64") String createdBy,
        LocalDateTime updatedAt,
        @Schema(format = "int64") String updatedBy) {
}
