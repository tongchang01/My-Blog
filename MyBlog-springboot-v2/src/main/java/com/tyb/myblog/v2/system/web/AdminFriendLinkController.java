package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkPageResult;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台友链管理接口。
 */
@Tag(name = "后台友链", description = "友链管理")
@RestController
@RequestMapping("/api/admin/friend-links")
@RequiredArgsConstructor
public class AdminFriendLinkController {

    private final FriendLinkQueryService queryService;

    @GetMapping
    public ApiResponse<PageResponse<AdminFriendLinkVO>> page(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        FriendLinkPageResult result =
                queryService.adminPage(principal, page, size);
        return ApiResponse.ok(new PageResponse<>(
                result.records().stream()
                        .map(AdminFriendLinkVO::from)
                        .toList(),
                result.total(),
                result.page(),
                result.size()));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<AdminFriendLinkVO> detail(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        return ApiResponse.ok(AdminFriendLinkVO.from(
                queryService.adminDetail(principal, id)));
    }
}
