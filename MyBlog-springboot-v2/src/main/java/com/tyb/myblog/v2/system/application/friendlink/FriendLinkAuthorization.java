package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import org.springframework.stereotype.Component;

/**
 * 友链用例的角色和审计用户校验。
 */
@Component
class FriendLinkAuthorization {

    void requireReadable(AuthenticatedPrincipal principal) {
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

    long requireAdmin(AuthenticatedPrincipal principal) {
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
