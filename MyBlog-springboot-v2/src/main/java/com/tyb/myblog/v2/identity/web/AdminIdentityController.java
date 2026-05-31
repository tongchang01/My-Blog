package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.identity.application.IdentityQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/user")
public class AdminIdentityController {

    private final IdentityQueryService identityQueryService;

    public AdminIdentityController(IdentityQueryService identityQueryService) {
        this.identityQueryService = identityQueryService;
    }

    @GetMapping("/menus")
    ApiResponse<List<UserMenuResponse>> menus(@CurrentUser AuthenticatedPrincipal user) {
        List<UserMenuResponse> menus = identityQueryService.currentUserMenus(user)
                .stream()
                .map(UserMenuResponse::from)
                .toList();
        return ApiResponse.ok(menus);
    }
}
