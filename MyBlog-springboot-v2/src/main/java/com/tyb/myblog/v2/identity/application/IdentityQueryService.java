package com.tyb.myblog.v2.identity.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.CurrentUserProfile;
import com.tyb.myblog.v2.identity.domain.CurrentUserProfileReader;
import com.tyb.myblog.v2.identity.domain.UserMenu;
import com.tyb.myblog.v2.identity.domain.UserMenuReader;
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
