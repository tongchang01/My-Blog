package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.identity.application.IdentityQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台用户身份接口。
 *
 * <p>当前只暴露后台菜单查询能力，调用方必须是已认证且具备后台访问权限的用户。</p>
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminIdentityController {

    private final IdentityQueryService identityQueryService;

    public AdminIdentityController(IdentityQueryService identityQueryService) {
        this.identityQueryService = identityQueryService;
    }

    /**
     * 查询当前登录用户可访问的后台菜单树。
     */
    @GetMapping("/menus")
    ApiResponse<List<UserMenuResponse>> menus(@CurrentUser AuthenticatedPrincipal user) {
        List<UserMenuResponse> menus = identityQueryService.currentUserMenus(user)
                .stream()
                .map(UserMenuResponse::from)
                .toList();
        return ApiResponse.ok(menus);
    }
}
