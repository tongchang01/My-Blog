package com.tyb.myblog.v2.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.system.infrastructure.persistence.entity.AttachmentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 附件持久化 Mapper，SQL 统一位于 XML。
 */
@Mapper
public interface AttachmentMapper extends BaseMapper<AttachmentEntity> {

    AttachmentEntity selectActiveById(@Param("id") long id);

    AttachmentEntity selectActiveByIdForUpdate(@Param("id") long id);

    List<AttachmentEntity> selectActiveByIds(
            @Param("ids") List<Long> ids);

    AttachmentEntity selectByHashIncludingDeleted(@Param("hash") String hash);

    List<AttachmentEntity> selectActivePage(
            @Param("offset") long offset,
            @Param("size") int size);

    long countActive();

    List<AttachmentEntity> selectDeletedPage(
            @Param("offset") long offset,
            @Param("size") int size);

    long countDeleted();

    int softDelete(
            @Param("id") long id,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deletedBy") long deletedBy);

    int restoreDeleted(
            @Param("id") long id,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);
}
