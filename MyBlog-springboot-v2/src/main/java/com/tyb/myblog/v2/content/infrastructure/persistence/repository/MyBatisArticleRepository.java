package com.tyb.myblog.v2.content.infrastructure.persistence.repository;

import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticleRepository;
import com.tyb.myblog.v2.content.domain.article.HomepageSlot;
import com.tyb.myblog.v2.content.domain.article.NewArticle;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.ArticleEntity;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ArticleMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapping.ArticlePersistenceMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的文章聚合仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisArticleRepository implements ArticleRepository {

    private final ArticleMapper mapper;
    private final ArticlePersistenceMapping mapping;

    @Override
    public Article insert(NewArticle article) {
        ArticleEntity entity = mapping.toEntity(article);
        if (mapper.insert(entity) != 1
                || entity.getId() == null
                || entity.getId() <= 0) {
            throw new IllegalStateException("文章写入失败");
        }
        return mapping.toDomain(entity, article.tagIds());
    }

    @Override
    public Optional<Article> findActiveById(long id) {
        return toDomain(mapper.selectActiveById(id));
    }

    @Override
    public Optional<Article> findActiveByIdForUpdate(long id) {
        return toDomain(mapper.selectActiveByIdForUpdate(id));
    }

    @Override
    public Optional<Article> findDeletedByIdForUpdate(long id) {
        return toDomain(mapper.selectDeletedByIdForUpdate(id));
    }

    @Override
    public boolean update(
            Article article,
            LocalDateTime updatedAt,
            Long updatedBy) {
        return mapper.updateActive(
                mapping.toEntity(article),
                updatedAt,
                updatedBy) == 1;
    }

    @Override
    public int countActiveHomepageSlot(
            HomepageSlot slot,
            Long excludeArticleId) {
        return mapper.countActiveHomepageSlot(slot, excludeArticleId);
    }

    @Override
    public boolean softDelete(
            long id,
            LocalDateTime deletedAt,
            long deletedBy) {
        return mapper.softDelete(id, deletedAt, deletedBy) == 1;
    }

    @Override
    public boolean restore(
            long id,
            LocalDateTime updatedAt,
            long updatedBy) {
        return mapper.restore(id, updatedAt, updatedBy) == 1;
    }

    @Override
    public List<Article> findDueScheduledForUpdate(
            LocalDateTime now,
            int limit) {
        return mapper.selectDueScheduledForUpdate(now, limit)
                .stream()
                .map(entity -> mapping.toDomain(
                        entity,
                        mapper.selectTagIds(entity.getId())))
                .toList();
    }

    @Override
    public boolean updateStatus(
            long id,
            com.tyb.myblog.v2.content.domain.article.ArticleStatus expected,
            com.tyb.myblog.v2.content.domain.article.ArticleStatus target,
            LocalDateTime updatedAt,
            Long updatedBy) {
        return mapper.updateStatus(
                id,
                expected,
                target,
                updatedAt,
                updatedBy) == 1;
    }

    @Override
    public void replaceTags(long articleId, List<Long> tagIds) {
        mapper.deleteTagRelations(articleId);
        if (tagIds == null) {
            return;
        }
        tagIds.stream()
                .sorted()
                .forEach(tagId -> {
                    if (mapper.insertTagRelation(articleId, tagId) != 1) {
                        throw new IllegalStateException(
                                "文章标签关联写入失败");
                    }
                });
    }

    private Optional<Article> toDomain(ArticleEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(mapping.toDomain(
                entity,
                mapper.selectTagIds(entity.getId())));
    }
}
