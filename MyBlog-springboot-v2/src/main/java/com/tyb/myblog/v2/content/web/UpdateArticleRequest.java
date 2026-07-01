package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.article.UpdateArticleCommand;

/**
 * 完整编辑文章 HTTP 请求。
 */
public class UpdateArticleRequest extends ArticleWriteRequestSupport {

    public UpdateArticleCommand toCommand() {
        Values values = values();
        return new UpdateArticleCommand(
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
                values.homepageSlot(),
                values.password(),
                values.publishAt(),
                values.coverAttachmentId());
    }
}
