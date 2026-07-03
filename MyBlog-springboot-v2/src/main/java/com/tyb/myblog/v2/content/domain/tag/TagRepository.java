package com.tyb.myblog.v2.content.domain.tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 标签仓储端口。
 */
public interface TagRepository {

    List<Tag> findAllActive();

    List<PublicTagWithArticleCount> findPublicWithArticleCount(
            LocalDateTime now);

    Optional<Tag> findActiveById(long id);

    Optional<Tag> findActiveByIdForUpdate(long id);

    List<Tag> findActiveByIdsForUpdate(List<Long> ids);

    Optional<Tag> findBySlugIncludingDeleted(String slug);

    Tag insert(NewTag tag);

    boolean update(
            Tag tag,
            LocalDateTime updatedAt,
            long updatedBy);

    boolean hasActiveArticleReference(long tagId);

    boolean softDelete(
            long id,
            LocalDateTime deletedAt,
            long deletedBy);

    record PublicTagWithArticleCount(
            Tag tag,
            long articleCount) {
    }
}
