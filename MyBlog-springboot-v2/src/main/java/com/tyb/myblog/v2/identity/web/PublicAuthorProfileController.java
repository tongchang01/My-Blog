package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.identity.application.profile.PublicAuthorProfileQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台公开作者资料接口。
 */
@Tag(name = "公开作者资料", description = "前台作者卡片资料")
@RestController
@RequestMapping("/api/public/author-profile")
@RequiredArgsConstructor
public class PublicAuthorProfileController {

    private final PublicAuthorProfileQueryService queryService;

    /**
     * 查询前台主作者资料。
     */
    @Operation(summary = "查询前台主作者资料")
    @GetMapping
    public ApiResponse<UserProfileVO> get() {
        return ApiResponse.ok(UserProfileVO.from(queryService.query()));
    }
}
