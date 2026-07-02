package com.tyb.myblog.v2.content.application.category;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.ContentLanguage;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开和后台分类查询服务。
 */
@Service
@RequiredArgsConstructor
public class CategoryQueryService {

    private final CategoryRepository repository;
    private final ContentAuthorization authorization;
    private final Clock clock;

    public List<PublicCategoryResult> publicList(
            String languageCode) {
        ContentLanguage language = parseLanguage(languageCode);
        return repository.findPublicWithArticleCount(
                        LocalDateTime.now(clock))
                .stream()
                .map(item -> new PublicCategoryResult(
                        item.category().id(),
                        item.category().name().localized(language),
                        item.category().slug().value(),
                        item.articleCount()))
                .toList();
    }

    public List<CategoryResult> adminList(
            AuthenticatedPrincipal principal) {
        authorization.requireReadable(principal);
        return repository.findAllActive().stream()
                .map(CategoryResult::from)
                .toList();
    }

    public CategoryResult adminDetail(
            AuthenticatedPrincipal principal,
            long id) {
        authorization.requireReadable(principal);
        validateId(id);
        return repository.findActiveById(id)
                .map(CategoryResult::from)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "分类不存在"));
    }

    private ContentLanguage parseLanguage(String value) {
        try {
            return ContentLanguage.parse(value);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "分类 ID 必须为正数");
        }
    }
}
