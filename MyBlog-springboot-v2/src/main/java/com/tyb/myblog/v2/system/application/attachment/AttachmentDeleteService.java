package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 后台附件软删除服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentDeleteService {

    private final AttachmentRepository repository;
    private final Clock clock;

    /**
     * 只标记附件为 deleted，不删除物理对象。
     */
    @Transactional
    public void delete(
            AuthenticatedPrincipal principal,
            long id) {
        long actorId = requireAdmin(principal);
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "附件 ID 必须为正数");
        }
        repository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "附件不存在"));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!repository.softDelete(id, now, actorId)) {
            log.error("附件软删除行数异常，attachmentId={}", id);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
    }

    @Transactional
    public void restore(
            AuthenticatedPrincipal principal,
            long id) {
        long actorId = requireAdmin(principal);
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "附件 ID 必须为正数");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!repository.restoreDeleted(id, now, actorId)) {
            throw new ApiException(
                    ApiErrorCode.NOT_FOUND,
                    "附件不存在");
        }
    }

    private long requireAdmin(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        if (!principal.roles().contains("ADMIN")) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        try {
            long id = Long.parseLong(principal.id());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
    }
}
