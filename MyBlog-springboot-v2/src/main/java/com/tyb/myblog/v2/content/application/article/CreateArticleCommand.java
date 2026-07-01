package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.HomepageSlot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台创建文章的完整字段命令。
 */
public record CreateArticleCommand(
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
        String body,
        Long categoryId,
        List<Long> tagIds,
        String slug,
        ArticleStatus status,
        String password,
        LocalDateTime publishAt,
        Long coverAttachmentId,
        HomepageSlot homepageSlot) {

    public CreateArticleCommand(
            String titleZh,
            String titleJa,
            String titleEn,
            String summaryZh,
            String summaryJa,
            String summaryEn,
            String body,
            Long categoryId,
            List<Long> tagIds,
            String slug,
            ArticleStatus status,
            String password,
            LocalDateTime publishAt,
            Long coverAttachmentId) {
        this(
                titleZh,
                titleJa,
                titleEn,
                summaryZh,
                summaryJa,
                summaryEn,
                body,
                categoryId,
                tagIds,
                slug,
                status,
                password,
                publishAt,
                coverAttachmentId,
                HomepageSlot.NONE);
    }

    public CreateArticleCommand {
        homepageSlot = HomepageSlot.normalize(homepageSlot);
    }
}
