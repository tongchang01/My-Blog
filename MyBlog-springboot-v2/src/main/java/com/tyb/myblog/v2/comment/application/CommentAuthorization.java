package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import org.springframework.stereotype.Component;

@Component
public class CommentAuthorization {

    public void requireReadable(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        boolean readable = principal.roles().stream()
                .anyMatch(role -> "ADMIN".equals(role) || "DEMO".equals(role));
        if (!readable) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    public long requireAdmin(AuthenticatedPrincipal principal) {
        requireReadable(principal);
        if (!principal.roles().contains("ADMIN")) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        try {
            long id = Long.parseLong(principal.id());
            if (id <= 0) {
                throw new ApiException(ApiErrorCode.FORBIDDEN);
            }
            return id;
        } catch (NumberFormatException ex) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
    }
}
