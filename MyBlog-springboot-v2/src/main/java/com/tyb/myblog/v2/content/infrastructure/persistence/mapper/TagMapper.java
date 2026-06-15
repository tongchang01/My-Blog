package com.tyb.myblog.v2.content.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.TagEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 标签持久化 Mapper，SQL 统一位于 XML。
 */
@Mapper
public interface TagMapper extends BaseMapper<TagEntity> {

    List<TagEntity> selectAllActive();

    TagEntity selectActiveById(@Param("id") long id);

    TagEntity selectActiveByIdForUpdate(@Param("id") long id);

    TagEntity selectBySlugIncludingDeleted(
            @Param("slug") String slug);
}
