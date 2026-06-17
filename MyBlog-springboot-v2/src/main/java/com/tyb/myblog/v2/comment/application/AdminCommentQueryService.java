package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.AdminCommentPage;
import com.tyb.myblog.v2.comment.domain.AdminCommentPageItem;
import com.tyb.myblog.v2.comment.domain.AdminCommentQueryCriteria;
import com.tyb.myblog.v2.comment.domain.AdminCommentQueryRepository;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminCommentQueryService {

    private final AdminCommentQueryRepository repository;
    private final CommentAuthorization authorization;

    public AdminCommentPageResult page(
            AuthenticatedPrincipal principal,
            AdminCommentPageQuery query) {
        authorization.requireReadable(principal);
        AdminCommentPage page = repository.page(toCriteria(query));
        return new AdminCommentPageResult(
                page.records().stream()
                        .map(AdminCommentQueryService::toItem)
                        .toList(),
                page.total(),
                page.page(),
                page.size());
    }

    private static AdminCommentQueryCriteria toCriteria(
            AdminCommentPageQuery query) {
        return new AdminCommentQueryCriteria(
                query.targetType(),
                query.targetId(),
                query.auditStatus(),
                query.keyword(),
                query.includeDeleted(),
                query.page(),
                query.size());
    }

    private static AdminCommentPageResult.Item toItem(
            AdminCommentPageItem item) {
        return new AdminCommentPageResult.Item(
                item.id(),
                item.targetType(),
                item.targetId(),
                item.parentId(),
                item.replyToCommentId(),
                item.replyToNickname(),
                item.authorNickname(),
                item.authorEmail(),
                item.authorSite(),
                item.authorIp(),
                item.authorUserAgent(),
                item.contentMd(),
                item.contentHtml(),
                item.auditStatus(),
                item.createdAt(),
                item.deleted());
    }
}
