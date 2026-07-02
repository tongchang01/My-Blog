package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PublicArticleQueryRepository {

    PublicArticlePage findPublicPage(PublicArticleCriteria criteria);

    PublicArticleHome findPublicHome(LocalDateTime now, int size);

    Optional<PublicArticleAccessMetadata> findPublicAccessMetadata(
            long id,
            LocalDateTime now);

    Optional<PublicArticleDetail> findPublicDetail(
            long id,
            LocalDateTime now);
}
