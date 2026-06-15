package com.tyb.myblog.v2.content.application.category;

/**
 * 新增分类命令。
 */
public record CreateCategoryCommand(
        String nameZh,
        String nameJa,
        String nameEn,
        String slug,
        int sortOrder) {
}
