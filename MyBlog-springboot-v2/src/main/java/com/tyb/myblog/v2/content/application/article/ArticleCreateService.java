package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticlePasswordHasher;
import com.tyb.myblog.v2.content.domain.article.ArticleRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.NewArticle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 后台文章创建用例。
 */
@Service
@RequiredArgsConstructor
public class ArticleCreateService {

    private final ArticleRepository repository;
    private final ContentAuthorization authorization;
    private final ArticleReferenceValidator referenceValidator;
    private final ArticlePasswordHasher hasher;
    private final Clock clock;

    @Transactional
    public ArticleResult create(
            AuthenticatedPrincipal principal,
            CreateArticleCommand command) {
        long actorId = authorization.requireAdmin(principal);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime publishAt = normalizePublishAt(
                command.status(), command.publishAt(), now);
        String hashedPassword = resolvePassword(command);
        referenceValidator.lockAndValidate(
                command.status(),
                command.categoryId(),
                command.tagIds(),
                command.coverAttachmentId());
        NewArticle candidate = NewArticle.create(
                command.titleZh(),
                command.titleJa(),
                command.titleEn(),
                command.summaryZh(),
                command.summaryJa(),
                command.summaryEn(),
                command.body(),
                command.categoryId(),
                actorId,
                command.slug(),
                command.status(),
                hashedPassword,
                publishAt,
                command.coverAttachmentId(),
                command.tagIds(),
                actorId,
                now);
        Article inserted = repository.insert(candidate);
        repository.replaceTags(inserted.id(), inserted.tagIds());
        return ArticleResult.from(inserted);
    }

    private String resolvePassword(CreateArticleCommand command) {
        if (command.status() == ArticleStatus.PASSWORD) {
            return hasher.hash(command.password());
        }
        if (command.password() != null && !command.password().isBlank()) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "非密码文章不得设置访问密码");
        }
        return null;
    }

    private LocalDateTime normalizePublishAt(
            ArticleStatus status,
            LocalDateTime requested,
            LocalDateTime now) {
        LocalDateTime publishAt;
        if (status == ArticleStatus.PUBLISHED
                || status == ArticleStatus.PASSWORD) {
            publishAt = requested == null ? now : requested;
        } else {
            publishAt = requested;
        }
        return publishAt == null ? null : publishAt.withNano(0);
    }
}
