package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.application.attachment.AttachmentResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 后台附件响应。
 */
public record AttachmentVO(
        @Schema(format = "int64") String id,
        String publicUrl,
        String contentType,
        long fileSize,
        int width,
        int height,
        String originalFilename,
        LocalDateTime createdAt,
        @Schema(format = "int64") String createdBy
) {
    public static AttachmentVO from(AttachmentResult result) {
        return new AttachmentVO(
                Long.toString(result.id()),
                result.publicUrl(),
                result.contentType(),
                result.fileSize(),
                result.width(),
                result.height(),
                result.originalFilename(),
                result.createdAt(),
                result.createdBy() == null
                        ? null
                        : Long.toString(result.createdBy()));
    }
}
