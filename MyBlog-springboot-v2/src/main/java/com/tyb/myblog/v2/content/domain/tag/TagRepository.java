package com.tyb.myblog.v2.content.domain.tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 标签仓储端口。
 */
public interface TagRepository {

    List<Tag> findAllActive();

    Optional<Tag> findActiveById(long id);

    Optional<Tag> findActiveByIdForUpdate(long id);

    Optional<Tag> findBySlugIncludingDeleted(String slug);

    Tag insert(NewTag tag);

    boolean update(
            Tag tag,
            LocalDateTime updatedAt,
            long updatedBy);
}
