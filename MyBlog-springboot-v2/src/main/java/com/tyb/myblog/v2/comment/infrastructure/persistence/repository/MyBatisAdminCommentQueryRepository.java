package com.tyb.myblog.v2.comment.infrastructure.persistence.repository;

import com.tyb.myblog.v2.comment.domain.AdminCommentPage;
import com.tyb.myblog.v2.comment.domain.AdminCommentPageItem;
import com.tyb.myblog.v2.comment.domain.AdminCommentQueryCriteria;
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
    public AdminCommentPage page(AdminCommentQueryCriteria criteria) {
        long offset = (long) (criteria.page() - 1) * criteria.size();
        Integer targetType = criteria.targetType() == null
                ? null
                : criteria.targetType().databaseValue();
        Integer auditStatus = criteria.auditStatus() == null
                ? null
                : criteria.auditStatus().databaseValue();
        return new AdminCommentPage(
                mapper.selectAdminPage(
                                targetType,
                                criteria.targetId(),
                                auditStatus,
                                normalizeKeyword(criteria.keyword()),
                                criteria.includeDeleted(),
                                offset,
                                criteria.size())
                        .stream()
                        .map(MyBatisAdminCommentQueryRepository::toItem)
                        .toList(),
                mapper.countAdminPage(
                        targetType,
                        criteria.targetId(),
                        auditStatus,
                        normalizeKeyword(criteria.keyword()),
                        criteria.includeDeleted()),
                criteria.page(),
                criteria.size());
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
