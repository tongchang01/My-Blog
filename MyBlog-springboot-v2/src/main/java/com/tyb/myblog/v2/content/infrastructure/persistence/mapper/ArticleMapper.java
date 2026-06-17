package com.tyb.myblog.v2.content.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.content.domain.article.AdminArticleCriteria;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.PublicArticleCriteria;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.ArticleEntity;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.ArticleTagRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.AdminArticleDetailRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.AdminArticlePageRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.ArticleCommentPolicyRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.DeletedArticlePageRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.PublicArticleDetailRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.PublicArticlePageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章持久化 Mapper，生产 SQL 统一位于 XML。
 */
@Mapper
public interface ArticleMapper extends BaseMapper<ArticleEntity> {

    ArticleEntity selectActiveById(@Param("id") long id);

    ArticleEntity selectActiveByIdForUpdate(@Param("id") long id);

    ArticleEntity selectDeletedByIdForUpdate(@Param("id") long id);

    List<Long> selectTagIds(@Param("articleId") long articleId);

    List<AdminArticlePageRow> selectAdminPage(
            @Param("query") AdminArticleCriteria query,
            @Param("offset") long offset,
            @Param("size") int size);

    long countAdminPage(@Param("query") AdminArticleCriteria query);

    AdminArticleDetailRow selectAdminDetail(@Param("id") long id);

    List<PublicArticlePageRow> selectPublicPage(
            @Param("query") PublicArticleCriteria query,
            @Param("offset") long offset,
            @Param("size") int size);

    long countPublicPage(@Param("query") PublicArticleCriteria query);

    PublicArticleDetailRow selectPublicDetail(
            @Param("id") long id,
            @Param("now") LocalDateTime now);

    ArticleCommentPolicyRow selectCommentPolicy(@Param("id") long id);

    int incrementCommentCount(
            @Param("id") long id,
            @Param("delta") int delta);

    List<ArticleTagRow> selectPublicArticleTags(
            @Param("articleIds") List<Long> articleIds);

    List<DeletedArticlePageRow> selectDeletedPage(
            @Param("offset") long offset,
            @Param("size") int size);

    long countDeletedPage();

    List<ArticleEntity> selectDueScheduledForUpdate(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    int updateActive(
            @Param("article") ArticleEntity article,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") Long updatedBy);

    int softDelete(
            @Param("id") long id,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deletedBy") long deletedBy);

    int restore(
            @Param("id") long id,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);

    int updateStatus(
            @Param("id") long id,
            @Param("expected") ArticleStatus expected,
            @Param("target") ArticleStatus target,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") Long updatedBy);

    int deleteTagRelations(@Param("articleId") long articleId);

    int insertTagRelation(
            @Param("articleId") long articleId,
            @Param("tagId") long tagId);
}
