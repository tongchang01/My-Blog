package com.tyb.myblog.v2.content.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.ArticleEntity;
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

    int updateActive(
            @Param("article") ArticleEntity article,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") Long updatedBy);

    int deleteTagRelations(@Param("articleId") long articleId);

    int insertTagRelation(
            @Param("articleId") long articleId,
            @Param("tagId") long tagId);
}
