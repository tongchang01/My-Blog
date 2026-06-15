package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.category.CreateCategoryCommand;

/**
 * 新增分类 HTTP 请求。
 */
public class CreateCategoryRequest
        extends CategoryWriteRequestSupport {

    public CreateCategoryCommand toCommand() {
        Values values = values();
        return new CreateCategoryCommand(
                values.nameZh(),
                values.nameJa(),
                values.nameEn(),
                values.slug(),
                values.sortOrder());
    }
}
