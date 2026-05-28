package com.aurora.myblog.v2.modules.identity.application;

import com.aurora.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.identity.domain.CurrentUserProfile;
import com.aurora.myblog.v2.modules.identity.domain.CurrentUserProfileReader;
import com.aurora.myblog.v2.modules.identity.domain.UserMenu;
import com.aurora.myblog.v2.modules.identity.domain.UserMenuReader;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IdentityQueryService {

    private final CurrentUserProfileReader profileReader;
    private final UserMenuReader userMenuReader;

    public IdentityQueryService(CurrentUserProfileReader profileReader, UserMenuReader userMenuReader) {
        this.profileReader = profileReader;
        this.userMenuReader = userMenuReader;
    }

    public CurrentUserProfile currentProfile(AuthenticatedPrincipal principal) {
        return profileReader.findByAuthId(principal.id())
                .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_REQUIRED, "用户未登录"));
    }

    public List<UserMenu> currentUserMenus(AuthenticatedPrincipal principal) {
        return userMenuReader.findByAuthId(principal.id());
    }
}
