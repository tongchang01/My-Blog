package com.tyb.myblog.v2.content.application.article;

import java.util.Optional;

public interface ArticleCommentGateway {

    Optional<ArticleCommentPolicySnapshot> findCommentPolicy(long articleId);

    boolean incrementCommentCount(long articleId, int delta);
}
