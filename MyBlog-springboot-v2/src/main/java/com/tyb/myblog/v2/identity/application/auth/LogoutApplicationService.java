package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.application.token.UserTokenRevocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 执行当前后台账号的全端退出。
 */
@Service
@RequiredArgsConstructor
public class LogoutApplicationService {

    private final UserTokenRevocationService revocationService;

    /**
     * 撤销当前账号全部 access token 和 refresh token。
     */
    public void logout(String principalId) {
        long userId = parsePositiveUserId(principalId);
        if (!revocationService.revokeAll(userId, userId)) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
    }

    private long parsePositiveUserId(String principalId) {
        try {
            long userId = Long.parseLong(principalId);
            if (userId <= 0) {
                throw new NumberFormatException();
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
    }
}
