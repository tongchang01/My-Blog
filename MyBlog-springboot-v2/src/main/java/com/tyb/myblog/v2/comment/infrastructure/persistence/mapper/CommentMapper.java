package com.tyb.myblog.v2.comment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.infrastructure.persistence.entity.CommentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface CommentMapper extends BaseMapper<CommentEntity> {

    CommentEntity selectActiveById(@Param("id") long id);

    CommentEntity selectDeletedByIdForUpdate(@Param("id") long id);

    int updateAuditStatus(
            @Param("id") long id,
            @Param("status") CommentAuditStatus status,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);

    int softDelete(
            @Param("id") long id,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deletedBy") long deletedBy);

    int restore(
            @Param("id") long id,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);
}
