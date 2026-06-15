package com.tyb.myblog.v2.content.application.category;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.ContentSlugConflictException;
import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 后台分类完整编辑服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryUpdateService {

    private final CategoryRepository repository;
    private final ContentAuthorization authorization;
    private final Clock clock;

    /**
     * 锁定 active 分类后完整替换业务字段，避免并发写入互相覆盖。
     */
    @Transactional
    public CategoryResult update(
            AuthenticatedPrincipal principal,
            long id,
            UpdateCategoryCommand command) {
        long actorId = authorization.requireAdmin(principal);
        validateRequest(id, command);
        Category current = repository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "分类不存在"));
        try {
            Category replacement = current.replace(
                    command.nameZh(),
                    command.nameJa(),
                    command.nameEn(),
                    command.slug(),
                    command.sortOrder());
            ensureSlugAvailable(replacement.slug().value(), id);
            LocalDateTime now = LocalDateTime.now(clock);
            if (!repository.update(replacement, now, actorId)) {
                log.error("分类更新行数异常，categoryId={}", id);
                throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
            }
            return repository.findActiveById(id)
                    .map(CategoryResult::from)
                    .orElseThrow(() -> {
                        log.error("分类更新后无法重新读取，categoryId={}", id);
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
                .filter(category -> category.id() != currentId)
                .ifPresent(category -> {
                    throw new ContentSlugConflictException();
                });
    }

    private void validateRequest(
            long id,
            UpdateCategoryCommand command) {
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "分类 ID 必须为正数");
        }
        if (command == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "分类请求不能为空");
        }
    }
}
