package com.tyb.myblog.v2.content.application.tag;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 标签引用保护软删除服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagDeleteService {

    private final TagRepository repository;
    private final ContentAuthorization authorization;
    private final Clock clock;

    @Transactional
    public void delete(
            AuthenticatedPrincipal principal,
            long id) {
        long actorId = authorization.requireAdmin(principal);
        validateId(id);
        repository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "标签不存在"));
        if (repository.hasActiveArticleReference(id)) {
            throw new ApiException(
                    ApiErrorCode.CONFLICT,
                    "标签仍被文章引用");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!repository.softDelete(id, now, actorId)) {
            log.error("标签软删除行数异常，tagId={}", id);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
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
