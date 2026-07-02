package com.tyb.myblog.v2.content.application.tag;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.ContentLanguage;
import com.tyb.myblog.v2.content.domain.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开和后台标签查询服务。
 */
@Service
@RequiredArgsConstructor
public class TagQueryService {

    private final TagRepository repository;
    private final ContentAuthorization authorization;
    private final Clock clock;

    public List<PublicTagResult> publicList(
            String languageCode) {
        ContentLanguage language = parseLanguage(languageCode);
        return repository.findPublicWithArticleCount(
                        LocalDateTime.now(clock))
                .stream()
                .map(item -> new PublicTagResult(
                        item.tag().id(),
                        item.tag().name().localized(language),
                        item.tag().slug().value(),
                        item.articleCount()))
                .toList();
    }

    public List<TagResult> adminList(
            AuthenticatedPrincipal principal) {
        authorization.requireReadable(principal);
        return repository.findAllActive().stream()
                .map(TagResult::from)
                .toList();
    }

    public TagResult adminDetail(
            AuthenticatedPrincipal principal,
            long id) {
        authorization.requireReadable(principal);
        validateId(id);
        return repository.findActiveById(id)
                .map(TagResult::from)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "标签不存在"));
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
                    "标签 ID 必须为正数");
        }
    }
}
