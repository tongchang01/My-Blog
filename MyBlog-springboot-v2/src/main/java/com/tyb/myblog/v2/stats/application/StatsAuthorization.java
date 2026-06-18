package com.tyb.myblog.v2.stats.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import org.springframework.stereotype.Component;

/** stats 用例共用的后台读取权限校验。 */
@Component
public class StatsAuthorization {

    public void requireReadable(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        boolean readable = principal.roles().stream()
                .anyMatch(role -> "ADMIN".equals(role)
                        || "DEMO".equals(role));
        if (!readable) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }
}
