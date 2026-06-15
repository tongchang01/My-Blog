package com.tyb.myblog.v2.content.web;

import java.time.LocalDateTime;

/**
 * 后台分类响应。
 */
public record AdminCategoryVO(
        long id,
        String nameZh,
        String nameJa,
        String nameEn,
        String slug,
        int sortOrder,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {
}
