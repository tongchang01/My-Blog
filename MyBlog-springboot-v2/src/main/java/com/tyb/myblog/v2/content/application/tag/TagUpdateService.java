package com.tyb.myblog.v2.content.application.tag;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.ContentSlugConflictException;
import com.tyb.myblog.v2.content.domain.tag.Tag;
import com.tyb.myblog.v2.content.domain.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 后台标签完整编辑服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagUpdateService {

    private final TagRepository repository;
    private final ContentAuthorization authorization;
    private final Clock clock;

    /**
     * 锁定 active 标签后完整替换业务字段，避免编辑与删除交叉覆盖。
     */
    @Transactional
    public TagResult update(
            AuthenticatedPrincipal principal,
            long id,
            UpdateTagCommand command) {
        long actorId = authorization.requireAdmin(principal);
        validateRequest(id, command);
        Tag current = repository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "标签不存在"));
        try {
            Tag replacement = current.replace(
                    command.nameZh(),
                    command.nameJa(),
                    command.nameEn(),
                    command.slug());
            rejectSlugChange(current, replacement);
            ensureSlugAvailable(replacement.slug().value(), id);
            LocalDateTime now = LocalDateTime.now(clock);
            if (!repository.update(replacement, now, actorId)) {
                log.error("标签更新行数异常，tagId={}", id);
                throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
            }
            return repository.findActiveById(id)
                    .map(TagResult::from)
                    .orElseThrow(() -> {
                        log.error("标签更新后无法重新读取，tagId={}", id);
                        return new ApiException(
                                ApiErrorCode.INTERNAL_ERROR);
                    });
        } catch (ContentSlugConflictException exception) {
            throw new ApiException(ApiErrorCode.CONFLICT);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }
    }

    private void ensureSlugAvailable(String slug, long currentId) {
        repository.findBySlugIncludingDeleted(slug)
                .filter(tag -> tag.id() != currentId)
                .ifPresent(tag -> {
                    throw new ContentSlugConflictException();
                });
    }

    private void rejectSlugChange(Tag current, Tag replacement) {
        if (!current.slug().value().equals(replacement.slug().value())) {
            throw new ApiException(
                    ApiErrorCode.CONFLICT,
                    "标签 slug 创建后不能修改");
        }
    }

    private void validateRequest(long id, UpdateTagCommand command) {
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "标签 ID 必须为正数");
        }
        if (command == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "标签请求不能为空");
        }
    }
}
