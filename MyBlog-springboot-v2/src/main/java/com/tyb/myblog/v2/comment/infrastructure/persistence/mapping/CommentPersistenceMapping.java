package com.tyb.myblog.v2.comment.infrastructure.persistence.mapping;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentAuthor;
import com.tyb.myblog.v2.comment.domain.CommentContent;
import com.tyb.myblog.v2.comment.domain.CommentTarget;
import com.tyb.myblog.v2.comment.domain.CommentTargetType;
import com.tyb.myblog.v2.comment.domain.NewComment;
import com.tyb.myblog.v2.comment.infrastructure.persistence.entity.CommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentPersistenceMapping {

    default Comment toDomain(CommentEntity entity) {
        return Comment.reconstitute(
                entity.getId(),
                CommentTarget.of(
                        CommentTargetType.fromDatabase(entity.getTargetType()),
                        entity.getTargetId()),
                entity.getParentId(),
                entity.getReplyToCommentId(),
                entity.getReplyToUserId(),
                entity.getReplyToNickname(),
                new CommentAuthor(
                        entity.getAuthorUserId(),
                        entity.getAuthorNickname(),
                        entity.getAuthorEmail(),
                        entity.getAuthorSite(),
                        entity.getAuthorIp(),
                        entity.getAuthorUserAgent()),
                CommentContent.of(
                        entity.getContentMd(),
                        entity.getContentHtml()),
                CommentAuditStatus.fromDatabase(entity.getAuditStatus()),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                Integer.valueOf(1).equals(entity.getDeleted()),
                entity.getDeletedAt(),
                entity.getDeletedBy());
    }

    default CommentEntity toEntity(NewComment comment) {
        CommentEntity entity = new CommentEntity();
        entity.setTargetType(comment.target().targetType().databaseValue());
        entity.setTargetId(comment.target().targetId());
        entity.setParentId(comment.parentId());
        entity.setReplyToCommentId(comment.replyToCommentId());
        entity.setReplyToUserId(comment.replyToUserId());
        entity.setReplyToNickname(comment.replyToNickname());
        entity.setAuthorUserId(comment.author().userId());
        entity.setAuthorNickname(comment.author().nickname());
        entity.setAuthorEmail(comment.author().email());
        entity.setAuthorSite(comment.author().site());
        entity.setAuthorIp(comment.author().ip());
        entity.setAuthorUserAgent(comment.author().userAgent());
        entity.setContentMd(comment.content().markdown());
        entity.setContentHtml(comment.content().safeHtml());
        entity.setAuditStatus(comment.auditStatus().databaseValue());
        entity.setCreatedAt(comment.createdAt());
        entity.setCreatedBy(comment.createdBy());
        entity.setUpdatedAt(comment.createdAt());
        entity.setUpdatedBy(comment.createdBy());
        return entity;
    }
}
