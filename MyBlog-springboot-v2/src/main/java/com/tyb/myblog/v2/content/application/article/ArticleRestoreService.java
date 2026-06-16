package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ArticleRestoreService {

    private final ArticleRepository repository;
    private final ArticleReferenceValidator referenceValidator;
    private final ContentAuthorization authorization;
    private final Clock clock;

    @Transactional
    public ArticleResult restore(AuthenticatedPrincipal principal, long id) {
        long actorId = authorization.requireAdmin(principal);
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "文章 ID 必须为正数");
        }
        Article deleted = repository.findDeletedByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "文章不存在"));
        validateReferences(deleted);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!repository.restore(id, now, actorId)) {
            throw new ApiException(ApiErrorCode.CONFLICT);
        }
        return ArticleResult.from(deleted);
    }

    private void validateReferences(Article deleted) {
        try {
            referenceValidator.lockAndValidate(
                    deleted.status(),
                    deleted.categoryId(),
                    deleted.tagIds(),
                    deleted.coverAttachmentId());
        } catch (ApiException exception) {
            if (exception.code() == ApiErrorCode.NOT_FOUND) {
                throw new ApiException(
                        ApiErrorCode.CONFLICT,
                        "文章引用已失效，无法恢复");
            }
            throw exception;
        }
    }
}
