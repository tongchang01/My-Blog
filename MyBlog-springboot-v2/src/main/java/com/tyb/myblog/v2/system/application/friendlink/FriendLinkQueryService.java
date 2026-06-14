package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkPage;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公开和后台友链查询服务。
 */
@Service
@RequiredArgsConstructor
public class FriendLinkQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final FriendLinkRepository repository;
    private final FriendLinkAuthorization authorization;

    public List<PublicFriendLinkResult> publicList() {
        return repository.findPublicVisible().stream()
                .map(PublicFriendLinkResult::from)
                .toList();
    }

    public FriendLinkPageResult adminPage(
            AuthenticatedPrincipal principal,
            int page,
            int size) {
        authorization.requireReadable(principal);
        validatePage(page, size);
        FriendLinkPage result = repository.findActivePage(page, size);
        return new FriendLinkPageResult(
                result.records().stream()
                        .map(FriendLinkResult::from)
                        .toList(),
                result.total(),
                result.page(),
                result.size());
    }

    public FriendLinkResult adminDetail(
            AuthenticatedPrincipal principal,
            long id) {
        authorization.requireReadable(principal);
        validateId(id);
        return repository.findActiveById(id)
                .map(FriendLinkResult::from)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "友链不存在"));
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

    private void validateId(long id) {
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "友链 ID 必须为正数");
        }
    }
}
