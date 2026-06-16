package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.content.application.article.AdminArticleDetailResult;
import com.tyb.myblog.v2.content.application.article.AdminArticlePageResult;
import com.tyb.myblog.v2.content.application.article.AdminArticleQuery;
import com.tyb.myblog.v2.content.application.article.ArticleCreateService;
import com.tyb.myblog.v2.content.application.article.ArticleDeleteService;
import com.tyb.myblog.v2.content.application.article.ArticleQueryService;
import com.tyb.myblog.v2.content.application.article.ArticleRestoreService;
import com.tyb.myblog.v2.content.application.article.ArticleResult;
import com.tyb.myblog.v2.content.application.article.ArticleUpdateService;
import com.tyb.myblog.v2.content.application.article.DeletedArticlePageResult;
import com.tyb.myblog.v2.content.application.article.DeletedArticleQueryService;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 后台文章查询、创建与完整编辑接口。
 */
@Tag(name = "后台文章", description = "文章查询、创建与完整编辑")
@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleQueryService queryService;
    private final ArticleCreateService createService;
    private final ArticleUpdateService updateService;
    private final ArticleDeleteService deleteService;
    private final ArticleRestoreService restoreService;
    private final DeletedArticleQueryService deletedQueryService;
    private final ArticleWebMapping mapping;

    @GetMapping
    public ApiResponse<PageResponse<AdminArticlePageItemVO>> page(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String titleKeyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdTo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime publishFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime publishTo) {
        AdminArticlePageResult result = queryService.adminPage(
                principal,
                new AdminArticleQuery(
                        page,
                        size,
                        status,
                        categoryId,
                        tagId,
                        titleKeyword,
                        createdFrom,
                        createdTo,
                        publishFrom,
                        publishTo));
        return ApiResponse.ok(mapping.toAdminPage(result));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<AdminArticleDetailVO> detail(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        return ApiResponse.ok(mapping.toAdminDetail(
                queryService.adminDetail(principal, id)));
    }

    @GetMapping("/recycle-bin")
    public ApiResponse<PageResponse<DeletedArticlePageItemVO>> recycleBin(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        DeletedArticlePageResult result =
                deletedQueryService.page(principal, page, size);
        return ApiResponse.ok(mapping.toDeletedPage(result));
    }

    @PostMapping
    @Operation(
            summary = "新增文章",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(
                                    implementation =
                                            ArticleWriteOpenApiRequest.class))))
    public ApiResponse<AdminArticleDetailVO> create(
            @CurrentUser AuthenticatedPrincipal principal,
            @org.springframework.web.bind.annotation.RequestBody
            CreateArticleRequest request) {
        request.requireAllFields();
        ArticleResult created = createService.create(
                principal, request.toCommand());
        AdminArticleDetailResult detail =
                queryService.adminDetail(principal, created.id());
        return ApiResponse.ok(mapping.toAdminDetail(detail));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(
            summary = "完整编辑文章",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(
                                    implementation =
                                            ArticleWriteOpenApiRequest.class))))
    public ApiResponse<AdminArticleDetailVO> update(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id,
            @org.springframework.web.bind.annotation.RequestBody
            UpdateArticleRequest request) {
        request.requireAllFields();
        ArticleResult updated = updateService.update(
                principal, id, request.toCommand());
        AdminArticleDetailResult detail =
                queryService.adminDetail(principal, updated.id());
        return ApiResponse.ok(mapping.toAdminDetail(detail));
    }

    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> delete(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        deleteService.delete(principal, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id:\\d+}/restore")
    public ApiResponse<AdminArticleDetailVO> restore(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        ArticleResult restored = restoreService.restore(principal, id);
        AdminArticleDetailResult detail =
                queryService.adminDetail(principal, restored.id());
        return ApiResponse.ok(mapping.toAdminDetail(detail));
    }
}
