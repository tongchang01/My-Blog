package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ArticleMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.ArticleCommentPolicyRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleCommentPolicyService {

    private final ArticleMapper mapper;

    public ArticleCommentPolicy requirePublicCommentable(long articleId) {
        ArticleCommentPolicyRow row = mapper.selectCommentPolicy(articleId);
        if (row == null) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        ArticleStatus status = ArticleStatus.fromDatabase(row.getStatus());
        if (status == ArticleStatus.PASSWORD) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (status != ArticleStatus.PUBLISHED) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        return new ArticleCommentPolicy(
                row.getId(),
                row.getCommentCount() == null ? 0 : row.getCommentCount());
    }
}
