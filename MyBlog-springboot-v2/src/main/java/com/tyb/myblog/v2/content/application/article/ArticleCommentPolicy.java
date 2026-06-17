package com.tyb.myblog.v2.content.application.article;

public record ArticleCommentPolicy(
        long articleId,
        int commentCount) {
}
