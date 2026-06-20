package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.system.application.attachment.AttachmentPageResult;
import com.tyb.myblog.v2.system.application.attachment.AttachmentQueryService;
import com.tyb.myblog.v2.system.application.attachment.AttachmentResult;
import com.tyb.myblog.v2.system.application.attachment.AttachmentUploadCommand;
import com.tyb.myblog.v2.system.application.attachment.AttachmentUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 后台附件管理接口。
 */
@Tag(name = "后台附件", description = "附件上传与查询")
@RestController
@RequestMapping("/api/admin/attachments")
@RequiredArgsConstructor
public class AdminAttachmentController {

    private final AttachmentUploadService uploadService;
    private final AttachmentQueryService queryService;

    /**
     * 分页查询 active 附件。
     */
    @Operation(summary = "分页查询附件")
    @GetMapping
    public ApiResponse<PageResponse<AttachmentVO>> page(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        AttachmentPageResult result =
                queryService.page(principal, page, size);
        return ApiResponse.ok(new PageResponse<>(
                result.records().stream()
                        .map(AttachmentVO::from)
                        .toList(),
                result.total(),
                result.page(),
                result.size()));
    }

    /**
     * 查询单个 active 附件。
     */
    @Operation(summary = "查询附件详情")
    @GetMapping("/{id}")
    public ApiResponse<AttachmentVO> detail(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        return ApiResponse.ok(
                AttachmentVO.from(queryService.detail(principal, id)));
    }

    /**
     * 上传并登记图片附件。
     */
    @Operation(
            summary = "上传图片附件",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(
                                    implementation =
                                            AttachmentUploadOpenApiRequest.class))))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AttachmentVO> upload(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(AttachmentVO.from(
                uploadService.upload(
                        principal,
                        new AttachmentUploadCommand(
                                file.getOriginalFilename(),
                                file.getInputStream()))));
    }
}
