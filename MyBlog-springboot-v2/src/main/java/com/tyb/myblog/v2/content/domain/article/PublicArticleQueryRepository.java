package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PublicArticleQueryRepository {

    PublicArticlePage findPublicPage(PublicArticleCriteria criteria);

    Optional<PublicArticleDetail> findPublicDetail(
            long id,
            LocalDateTime now);
}
