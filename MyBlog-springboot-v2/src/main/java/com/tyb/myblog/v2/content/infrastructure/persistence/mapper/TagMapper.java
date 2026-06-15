package com.tyb.myblog.v2.content.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.TagEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标签持久化 Mapper，SQL 统一位于 XML。
 */
@Mapper
public interface TagMapper extends BaseMapper<TagEntity> {

    List<TagEntity> selectAllActive();

    TagEntity selectActiveById(@Param("id") long id);

    TagEntity selectActiveByIdForUpdate(@Param("id") long id);

    List<TagEntity> selectActiveByIdsForUpdate(
            @Param("ids") List<Long> ids);

    TagEntity selectBySlugIncludingDeleted(
            @Param("slug") String slug);

    int updateActive(
            @Param("tag") TagEntity tag,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);

    boolean existsActiveArticleReference(
            @Param("tagId") long tagId);

    int softDelete(
            @Param("id") long id,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deletedBy") long deletedBy);
}
