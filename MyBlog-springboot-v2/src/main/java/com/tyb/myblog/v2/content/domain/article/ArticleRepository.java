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

    int countActiveHomepageSlot(
            HomepageSlot slot,
            Long excludeArticleId);

    boolean softDelete(
            long id,
            LocalDateTime deletedAt,
            long deletedBy);

    boolean restore(
            long id,
            LocalDateTime updatedAt,
            long updatedBy);

    List<Article> findDueScheduledForUpdate(
            LocalDateTime now,
            int limit);

    boolean updateStatus(
            long id,
            ArticleStatus expected,
            ArticleStatus target,
            LocalDateTime updatedAt,
            Long updatedBy);

    void replaceTags(long articleId, List<Long> tagIds);
}
