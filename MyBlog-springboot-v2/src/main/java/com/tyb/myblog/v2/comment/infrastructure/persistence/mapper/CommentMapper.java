package com.tyb.myblog.v2.comment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.infrastructure.persistence.entity.CommentEntity;
import com.tyb.myblog.v2.comment.infrastructure.persistence.projection.AdminCommentPageRow;
import com.tyb.myblog.v2.comment.infrastructure.persistence.projection.CommentPageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<CommentEntity> {

    CommentEntity selectActiveById(@Param("id") long id);

    CommentEntity selectByIdIgnoringDeleted(@Param("id") long id);

    CommentEntity selectActiveByIdForUpdate(@Param("id") long id);

    CommentEntity selectByIdForUpdate(@Param("id") long id);

    List<Long> selectPublicReplyIdsForUpdate(@Param("rootId") long rootId);

    List<CommentPageRow> selectPublicRoots(
            @Param("targetType") int targetType,
            @Param("targetId") long targetId,
            @Param("offset") long offset,
            @Param("size") int size);

    List<CommentPageRow> selectPublicReplies(
            @Param("parentIds") List<Long> parentIds);

    long countPublicRoots(
            @Param("targetType") int targetType,
            @Param("targetId") long targetId);

    List<AdminCommentPageRow> selectAdminPage(
            @Param("targetType") Integer targetType,
            @Param("targetId") Long targetId,
            @Param("auditStatus") Integer auditStatus,
            @Param("keyword") String keyword,
            @Param("includeDeleted") boolean includeDeleted,
            @Param("offset") long offset,
            @Param("size") int size);

    long countAdminPage(
            @Param("targetType") Integer targetType,
            @Param("targetId") Long targetId,
            @Param("auditStatus") Integer auditStatus,
            @Param("keyword") String keyword,
            @Param("includeDeleted") boolean includeDeleted);

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
