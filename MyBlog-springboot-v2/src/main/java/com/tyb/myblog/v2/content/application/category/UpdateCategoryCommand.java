package com.tyb.myblog.v2.content.application.category;

/**
 * 完整编辑分类命令。
 */
public record UpdateCategoryCommand(
        String nameZh,
        String nameJa,
        String nameEn,
        String slug,
        int sortOrder) {
}
