package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.content.application.article.PublicArticleQuery;
import com.tyb.myblog.v2.content.application.article.PublicArticleQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/archives")
@RequiredArgsConstructor
public class PublicArchiveController {

    private final PublicArticleQueryService queryService;
    private final ArticleWebMapping mapping;

    @Operation(summary = "查询公开归档时间线")
    @GetMapping
    public ApiResponse<PageResponse<PublicArchivePageVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "zh") String lang) {
        return ApiResponse.ok(mapping.toPublicArchivePage(
                queryService.archives(new PublicArticleQuery(
                        page,
                        size,
                        lang,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))));
    }
}
