package com.aurora.myblog.v2.modules.identity.application;

import com.aurora.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.identity.domain.CurrentUserProfile;
import com.aurora.myblog.v2.modules.identity.domain.CurrentUserProfileReader;
import org.springframework.stereotype.Service;

@Service
public class IdentityQueryService {

    private final CurrentUserProfileReader profileReader;

    public IdentityQueryService(CurrentUserProfileReader profileReader) {
        this.profileReader = profileReader;
    }

    public CurrentUserProfile currentProfile(AuthenticatedPrincipal principal) {
        return profileReader.findByAuthId(principal.id())
                .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_REQUIRED, "用户未登录"));
    }
}
