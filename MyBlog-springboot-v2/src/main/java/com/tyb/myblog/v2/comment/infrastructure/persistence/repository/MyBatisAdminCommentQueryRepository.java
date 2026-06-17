package com.tyb.myblog.v2.comment.infrastructure.persistence.repository;

import com.tyb.myblog.v2.comment.application.AdminCommentPageQuery;
import com.tyb.myblog.v2.comment.domain.AdminCommentPage;
import com.tyb.myblog.v2.comment.domain.AdminCommentPageItem;
import com.tyb.myblog.v2.comment.domain.AdminCommentQueryRepository;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentTargetType;
import com.tyb.myblog.v2.comment.infrastructure.persistence.mapper.CommentMapper;
import com.tyb.myblog.v2.comment.infrastructure.persistence.projection.AdminCommentPageRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisAdminCommentQueryRepository
        implements AdminCommentQueryRepository {

    private final CommentMapper mapper;

    @Override
    public AdminCommentPage page(AdminCommentPageQuery query) {
        long offset = (long) (query.page() - 1) * query.size();
        Integer targetType = query.targetType() == null
                ? null
                : query.targetType().databaseValue();
        Integer auditStatus = query.auditStatus() == null
                ? null
                : query.auditStatus().databaseValue();
        return new AdminCommentPage(
                mapper.selectAdminPage(
                                targetType,
                                query.targetId(),
                                auditStatus,
                                normalizeKeyword(query.keyword()),
                                query.includeDeleted(),
                                offset,
                                query.size())
                        .stream()
                        .map(MyBatisAdminCommentQueryRepository::toItem)
                        .toList(),
                mapper.countAdminPage(
                        targetType,
                        query.targetId(),
                        auditStatus,
                        normalizeKeyword(query.keyword()),
                        query.includeDeleted()),
                query.page(),
                query.size());
    }

    private static AdminCommentPageItem toItem(AdminCommentPageRow row) {
        return new AdminCommentPageItem(
                row.id(),
                CommentTargetType.fromDatabase(row.targetType()),
                row.targetId(),
                row.parentId(),
                row.replyToCommentId(),
                row.replyToNickname(),
                row.authorNickname(),
                row.authorEmail(),
                row.authorSite(),
                row.authorIp(),
                row.authorUserAgent(),
                row.contentMd(),
                row.contentHtml(),
                CommentAuditStatus.fromDatabase(row.auditStatus()),
                row.createdAt(),
                Integer.valueOf(1).equals(row.deleted()));
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim() + "%";
    }
}
