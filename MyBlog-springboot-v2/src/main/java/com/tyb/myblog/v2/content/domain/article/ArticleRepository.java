package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文章聚合持久化端口。
 */
public interface ArticleRepository {

    Article insert(NewArticle article);

    Optional<Article> findActiveById(long id);

    Optional<Article> findActiveByIdForUpdate(long id);

    Optional<Article> findDeletedByIdForUpdate(long id);

    boolean update(
            Article article,
            LocalDateTime updatedAt,
            Long updatedBy);

    void replaceTags(long articleId, List<Long> tagIds);
}
