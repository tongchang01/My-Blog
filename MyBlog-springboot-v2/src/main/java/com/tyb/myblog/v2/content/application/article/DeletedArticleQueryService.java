package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.article.AdminArticleQueryRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleSortDirection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeletedArticleQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminArticleQueryRepository repository;
    private final ContentAuthorization authorization;

    public DeletedArticlePageResult page(
            AuthenticatedPrincipal principal,
            int page,
            int size,
            String sortBy,
            String sortDirection) {
        authorization.requireReadable(principal);
        if (page < 1) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "页码必须大于 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "每页数量必须在 1 到 100 之间");
        }
        return DeletedArticlePageResult.from(
                repository.findDeletedPage(
                        page,
                        size,
                        parseSort(sortBy, sortDirection)));
    }

    private ArticleSortDirection parseSort(
            String sortBy,
            String sortDirection) {
        if (!"deletedAt".equals(sortBy)) {
            throw invalidSort("sortBy");
        }
        if (sortDirection == null) {
            throw invalidSort("sortDirection");
        }
        return switch (sortDirection) {
            case "asc" -> ArticleSortDirection.ASC;
            case "desc" -> ArticleSortDirection.DESC;
            default -> throw invalidSort("sortDirection");
        };
    }

    private ApiException invalidSort(String parameter) {
        return new ApiException(
                ApiErrorCode.VALIDATION_ERROR,
                "排序参数非法: " + parameter);
    }
}
