package com.tyb.myblog.v2.content.infrastructure.persistence.mapping;

import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.NewArticle;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.ArticleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 文章领域对象与持久化实体的机械映射。
 *
 * <p>状态枚举和标签集合包含业务语义，保持显式转换。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ArticlePersistenceMapping {

    default Article toDomain(
            ArticleEntity entity,
            List<Long> tagIds) {
        return Article.reconstitute(
                entity.getId(),
                entity.getTitleZh(),
                entity.getTitleJa(),
                entity.getTitleEn(),
                entity.getSummaryZh(),
                entity.getSummaryJa(),
                entity.getSummaryEn(),
                entity.getBody(),
                entity.getCategoryId(),
                entity.getAuthorId(),
                entity.getSlug(),
                ArticleStatus.fromDatabase(entity.getStatus()),
                entity.getAccessPassword(),
                entity.getPublishAt(),
                entity.getCoverAttachmentId(),
                entity.getCommentCount(),
                tagIds,
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                Integer.valueOf(1).equals(entity.getDeleted()),
                entity.getDeletedAt(),
                entity.getDeletedBy());
    }

    default ArticleEntity toEntity(NewArticle article) {
        ArticleEntity entity = new ArticleEntity();
        copyBusinessFields(
                entity,
                article.titleZh(),
                article.titleJa(),
                article.titleEn(),
                article.summaryZh(),
                article.summaryJa(),
                article.summaryEn(),
                article.body(),
                article.categoryId(),
                article.authorId(),
                article.slug(),
                article.status(),
                article.accessPassword(),
                article.publishAt(),
                article.coverAttachmentId(),
                article.commentCount());
        entity.setCreatedBy(article.createdBy());
        entity.setUpdatedBy(article.createdBy());
        return entity;
    }

    default ArticleEntity toEntity(Article article) {
        ArticleEntity entity = new ArticleEntity();
        entity.setId(article.id());
        copyBusinessFields(
                entity,
                article.titleZh(),
                article.titleJa(),
                article.titleEn(),
                article.summaryZh(),
                article.summaryJa(),
                article.summaryEn(),
                article.body(),
                article.categoryId(),
                article.authorId(),
                article.slug(),
                article.status(),
                article.accessPassword(),
                article.publishAt(),
                article.coverAttachmentId(),
                article.commentCount());
        entity.setCreatedAt(article.createdAt());
        entity.setCreatedBy(article.createdBy());
        entity.setUpdatedAt(article.updatedAt());
        entity.setUpdatedBy(article.updatedBy());
        entity.setDeleted(article.deleted() ? 1 : 0);
        entity.setDeletedAt(article.deletedAt());
        entity.setDeletedBy(article.deletedBy());
        return entity;
    }

    private void copyBusinessFields(
            ArticleEntity entity,
            String titleZh,
            String titleJa,
            String titleEn,
            String summaryZh,
            String summaryJa,
            String summaryEn,
            String body,
            Long categoryId,
            long authorId,
            String slug,
            ArticleStatus status,
            String accessPassword,
            java.time.LocalDateTime publishAt,
            Long coverAttachmentId,
            int commentCount) {
        entity.setTitleZh(titleZh);
        entity.setTitleJa(titleJa);
        entity.setTitleEn(titleEn);
        entity.setSummaryZh(summaryZh);
        entity.setSummaryJa(summaryJa);
        entity.setSummaryEn(summaryEn);
        entity.setBody(body);
        entity.setCategoryId(categoryId);
        entity.setAuthorId(authorId);
        entity.setSlug(slug);
        entity.setStatus(status.databaseValue());
        entity.setAccessPassword(accessPassword);
        entity.setPublishAt(publishAt);
        entity.setCoverAttachmentId(coverAttachmentId);
        entity.setCommentCount(commentCount);
    }
}
