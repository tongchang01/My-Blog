package com.tyb.myblog.v2.content.application.category;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.ContentSlugConflictException;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
import com.tyb.myblog.v2.content.domain.category.NewCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台分类新增服务。
 */
@Service
@RequiredArgsConstructor
public class CategoryCreateService {

    private final CategoryRepository repository;
    private final ContentAuthorization authorization;

    @Transactional
    public CategoryResult create(
            AuthenticatedPrincipal principal,
            CreateCategoryCommand command) {
        long actorId = authorization.requireAdmin(principal);
        if (command == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "分类请求不能为空");
        }
        try {
            NewCategory candidate = NewCategory.create(
                    command.nameZh(),
                    command.nameJa(),
                    command.nameEn(),
                    command.slug(),
                    command.sortOrder(),
                    actorId);
            ensureSlugAvailable(candidate.slug().value());
            return CategoryResult.from(repository.insert(candidate));
        } catch (ContentSlugConflictException exception) {
            throw new ApiException(ApiErrorCode.CONFLICT);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }
    }

    private void ensureSlugAvailable(String slug) {
        if (repository.findBySlugIncludingDeleted(slug).isPresent()) {
            throw new ContentSlugConflictException();
        }
    }
}
