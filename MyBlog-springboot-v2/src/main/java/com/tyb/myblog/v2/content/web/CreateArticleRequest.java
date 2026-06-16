package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.article.CreateArticleCommand;

/**
 * 新增文章 HTTP 请求。
 */
public class CreateArticleRequest extends ArticleWriteRequestSupport {

    public CreateArticleCommand toCommand() {
        Values values = values();
        return new CreateArticleCommand(
                values.titleZh(),
                values.titleJa(),
                values.titleEn(),
                values.summaryZh(),
                values.summaryJa(),
                values.summaryEn(),
                values.body(),
                values.categoryId(),
                values.tagIds(),
                values.slug(),
                values.status(),
                values.password(),
                values.publishAt(),
                values.coverAttachmentId());
    }
}
