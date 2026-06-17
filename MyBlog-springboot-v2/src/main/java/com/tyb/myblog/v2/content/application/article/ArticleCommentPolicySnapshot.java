package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;

public record ArticleCommentPolicySnapshot(
        long articleId,
        ArticleStatus status,
        int commentCount) {
}
