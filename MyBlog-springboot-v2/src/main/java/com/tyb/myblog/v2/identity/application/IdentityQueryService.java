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

/**
 * 当前用户查询应用服务。
 *
 * <p>负责编排“我的资料”和“后台菜单”读取。调用方必须传入已认证主体，
 * 具体用户资料和菜单来源由 domain 端口隔离。</p>
 */
@Service
public class IdentityQueryService {

    private final CurrentUserProfileReader profileReader;
    private final UserMenuReader userMenuReader;

    public IdentityQueryService(CurrentUserProfileReader profileReader, UserMenuReader userMenuReader) {
        this.profileReader = profileReader;
        this.userMenuReader = userMenuReader;
    }

    /**
     * 查询当前登录用户资料。
     *
     * @param principal 当前认证主体
     * @return 用户资料
     */
    public CurrentUserProfile currentProfile(AuthenticatedPrincipal principal) {
        return profileReader.findByAuthId(principal.id())
                .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_REQUIRED, "用户未登录"));
    }

    /**
     * 查询当前登录用户可见的后台菜单树。
     *
     * @param principal 当前认证主体
     * @return 菜单树
     */
    public List<UserMenu> currentUserMenus(AuthenticatedPrincipal principal) {
        return userMenuReader.findByAuthId(principal.id());
    }
}
