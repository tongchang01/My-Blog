package com.tyb.myblog.v2.content.web;

import java.time.LocalDateTime;

/**
 * 后台标签响应。
 */
public record AdminTagVO(
        long id,
        String nameZh,
        String nameJa,
        String nameEn,
        String slug,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {
}
