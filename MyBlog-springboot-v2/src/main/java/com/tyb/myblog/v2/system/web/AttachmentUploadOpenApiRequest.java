package com.tyb.myblog.v2.system.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件上传 OpenAPI 文档模型。
 */
public record AttachmentUploadOpenApiRequest(
        @Schema(type = "string", format = "binary")
        MultipartFile file
) {
}
