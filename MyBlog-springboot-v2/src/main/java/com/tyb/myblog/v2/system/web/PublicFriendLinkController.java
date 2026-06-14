package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台公开友链接口。
 */
@Tag(name = "公开友链", description = "前台友链列表")
@RestController
@RequestMapping("/api/public/friend-links")
@RequiredArgsConstructor
public class PublicFriendLinkController {

    private final FriendLinkQueryService queryService;

    @GetMapping
    public ApiResponse<List<PublicFriendLinkVO>> list() {
        return ApiResponse.ok(queryService.publicList().stream()
                .map(PublicFriendLinkVO::from)
                .toList());
    }
}
