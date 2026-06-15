package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.category.UpdateCategoryCommand;

/**
 * 完整编辑分类 HTTP 请求。
 */
public class UpdateCategoryRequest
        extends CategoryWriteRequestSupport {

    public UpdateCategoryCommand toCommand() {
        Values values = values();
        return new UpdateCategoryCommand(
                values.nameZh(),
                values.nameJa(),
                values.nameEn(),
                values.slug(),
                values.sortOrder());
    }
}
