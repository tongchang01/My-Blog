package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticlePasswordHasher;
import com.tyb.myblog.v2.content.domain.article.ArticleRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.HomepageSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 后台文章完整编辑用例。
 */
@Service
@RequiredArgsConstructor
public class ArticleUpdateService {

    private final ArticleRepository repository;
    private final ContentAuthorization authorization;
    private final ArticleReferenceValidator referenceValidator;
    private final ArticlePasswordHasher hasher;
    private final PublicArticleAccessService accessService;
    private final Clock clock;

    @Transactional
    public ArticleResult update(
            AuthenticatedPrincipal principal,
            long id,
            UpdateArticleCommand command) {
        long actorId = authorization.requireAdmin(principal);
        HomepageSlot requestedSlot = HomepageSlot.normalize(
                command.homepageSlot());
        if (command.status() == ArticleStatus.PUBLISHED
                && requestedSlot != HomepageSlot.NONE) {
            repository.lockHomepageSlot(requestedSlot);
        }
        Article current = repository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "文章不存在"));
        LocalDateTime now = LocalDateTime.now(clock);
        String hashedPassword = resolvePassword(current, command);
        LocalDateTime publishAt = resolvePublishAt(current, command, now);
        HomepageSlot homepageSlot = resolveHomepageSlot(
                command.status(),
                requestedSlot,
                id);
        referenceValidator.lockAndValidate(
                command.status(),
                command.categoryId(),
                command.tagIds(),
                command.coverAttachmentId());
        Article replacement;
        try {
            replacement = current.replace(
                    command.titleZh(),
                    command.titleJa(),
                    command.titleEn(),
                    command.summaryZh(),
                    command.summaryJa(),
                    command.summaryEn(),
                    command.body(),
                    command.categoryId(),
                    command.slug(),
                    command.status(),
                    homepageSlot,
                    hashedPassword,
                    publishAt,
                    command.coverAttachmentId(),
                    command.tagIds(),
                    now,
                    actorId);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }
        if (!repository.update(replacement, now, actorId)) {
            throw new ApiException(ApiErrorCode.CONFLICT);
        }
        repository.replaceTags(id, replacement.tagIds());
        if (current.status() == ArticleStatus.PASSWORD
                && (replacement.status() != ArticleStatus.PASSWORD
                || !Objects.equals(current.accessPassword(), replacement.accessPassword()))) {
            accessService.revokeAll(id);
        }
        return ArticleResult.from(replacement);
    }

    private HomepageSlot resolveHomepageSlot(
            ArticleStatus status,
            HomepageSlot requested,
            long articleId) {
        HomepageSlot homepageSlot = HomepageSlot.normalize(requested);
        if (status != ArticleStatus.PUBLISHED) {
            return HomepageSlot.NONE;
        }
        if (homepageSlot == HomepageSlot.NONE) {
            return HomepageSlot.NONE;
        }
        if (repository.countActiveHomepageSlot(
                homepageSlot,
                articleId) >= homepageSlot.limit()) {
            throw new ApiException(
                    ApiErrorCode.CONFLICT,
                    "首页槽位数量已达上限");
        }
        return homepageSlot;
    }

    private String resolvePassword(
            Article current,
            UpdateArticleCommand command) {
        if (command.status() == ArticleStatus.PASSWORD) {
            if (command.password() == null) {
                return current.existingPassword();
            }
            return hasher.hash(command.password());
        }
        if (command.password() != null && !command.password().isBlank()) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "非密码文章不得设置访问密码");
        }
        return null;
    }

    private LocalDateTime resolvePublishAt(
            Article current,
            UpdateArticleCommand command,
            LocalDateTime now) {
        if (command.status() == ArticleStatus.PUBLISHED
                || command.status() == ArticleStatus.PASSWORD) {
            if (current.publishAt() != null) {
                return current.publishAt();
            }
            return command.publishAt() == null ? now : command.publishAt();
        }
        return command.publishAt();
    }
}
