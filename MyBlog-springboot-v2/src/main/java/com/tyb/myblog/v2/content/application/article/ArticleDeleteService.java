package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.article.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ArticleDeleteService {

    private final ArticleRepository repository;
    private final ContentAuthorization authorization;
    private final PublicArticleAccessService accessService;
    private final Clock clock;

    @Transactional
    public void delete(AuthenticatedPrincipal principal, long id) {
        long actorId = authorization.requireAdmin(principal);
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "文章 ID 必须为正数");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!repository.softDelete(id, now, actorId)) {
            throw new ApiException(
                    ApiErrorCode.NOT_FOUND,
                    "文章不存在");
        }
        accessService.revokeAll(id);
    }
}
