package com.tyb.myblog.v2.content.application.tag;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.ContentSlugConflictException;
import com.tyb.myblog.v2.content.domain.tag.NewTag;
import com.tyb.myblog.v2.content.domain.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台标签新增服务。
 */
@Service
@RequiredArgsConstructor
public class TagCreateService {

    private final TagRepository repository;
    private final ContentAuthorization authorization;

    @Transactional
    public TagResult create(
            AuthenticatedPrincipal principal,
            CreateTagCommand command) {
        long actorId = authorization.requireAdmin(principal);
        if (command == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "标签请求不能为空");
        }
        try {
            NewTag candidate = NewTag.create(
                    command.nameZh(),
                    command.nameJa(),
                    command.nameEn(),
                    command.slug(),
                    actorId);
            ensureSlugAvailable(candidate.slug().value());
            return TagResult.from(repository.insert(candidate));
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
