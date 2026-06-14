package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentPage;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 查询后台可见的 active 附件。
 */
@Service
@RequiredArgsConstructor
public class AttachmentQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AttachmentRepository repository;

    /**
     * 分页查询附件，允许 ADMIN 和 DEMO 只读访问。
     */
    public AttachmentPageResult page(
            AuthenticatedPrincipal principal,
            int page,
            int size) {
        requireReadableRole(principal);
        validatePage(page, size);
        AttachmentPage result = repository.findActivePage(page, size);
        return new AttachmentPageResult(
                result.records().stream()
                        .map(AttachmentResult::from)
                        .toList(),
                result.total(),
                result.page(),
                result.size());
    }

    /**
     * 查询单个 active 附件。
     */
    public AttachmentResult detail(
            AuthenticatedPrincipal principal,
            long id) {
        requireReadableRole(principal);
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "附件 ID 必须为正数");
        }
        return repository.findActiveById(id)
                .map(AttachmentResult::from)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "附件不存在"));
    }

    private void requireReadableRole(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        boolean readable = principal.roles().stream()
                .anyMatch(role ->
                        "ADMIN".equals(role) || "DEMO".equals(role));
        if (!readable) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    private void validatePage(int page, int size) {
        if (page < 1) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "页码必须大于 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "每页数量必须在 1 到 100 之间");
        }
    }
}
