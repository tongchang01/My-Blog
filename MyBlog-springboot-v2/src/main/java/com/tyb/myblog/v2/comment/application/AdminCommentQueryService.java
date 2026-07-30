package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.AdminCommentPage;
import com.tyb.myblog.v2.comment.domain.AdminCommentPageItem;
import com.tyb.myblog.v2.comment.domain.AdminCommentQueryCriteria;
import com.tyb.myblog.v2.comment.domain.AdminCommentQueryRepository;
import com.tyb.myblog.v2.comment.domain.CommentSortDirection;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
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
        boolean includeAuditFields = principal.isAdmin();
        return new AdminCommentPageResult(
                page.records().stream()
                        .map(item -> toItem(item, includeAuditFields))
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
                query.size(),
                parseSort(query.sortBy(), query.sortDirection()));
    }

    private static CommentSortDirection parseSort(
            String sortBy,
            String sortDirection) {
        if (!"createdAt".equals(sortBy)) {
            throw invalidSort("sortBy");
        }
        if (sortDirection == null) {
            throw invalidSort("sortDirection");
        }
        return switch (sortDirection) {
            case "asc" -> CommentSortDirection.ASC;
            case "desc" -> CommentSortDirection.DESC;
            default -> throw invalidSort("sortDirection");
        };
    }

    private static ApiException invalidSort(String parameter) {
        return new ApiException(
                ApiErrorCode.VALIDATION_ERROR,
                "排序参数非法: " + parameter);
    }

    private static AdminCommentPageResult.Item toItem(
            AdminCommentPageItem item,
            boolean includeAuditFields) {
        return new AdminCommentPageResult.Item(
                item.id(),
                item.targetType(),
                item.targetId(),
                item.parentId(),
                item.replyToCommentId(),
                item.replyToNickname(),
                item.authorNickname(),
                includeAuditFields ? item.authorEmail() : null,
                item.authorSite(),
                includeAuditFields ? item.authorIp() : null,
                includeAuditFields ? item.authorUserAgent() : null,
                item.contentMd(),
                item.contentHtml(),
                item.auditStatus(),
                item.createdAt(),
                item.deleted());
    }
}
