package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.content.application.tag.TagQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开标签查询接口。
 */
@Tag(name = "公开标签", description = "按语言读取标签")
@RestController
@RequestMapping("/api/public/tags")
@RequiredArgsConstructor
public class PublicTagController {

    private final TagQueryService queryService;
    private final TagWebMapping mapping;

    @GetMapping
    public ApiResponse<List<PublicTagVO>> list(
            @RequestParam String lang) {
        return ApiResponse.ok(
                queryService.publicList(lang).stream()
                        .map(mapping::toPublicVO)
                        .toList());
    }
}
