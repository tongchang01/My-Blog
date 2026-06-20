package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.application.attachment.AttachmentResult;

import java.time.LocalDateTime;

/**
 * 后台附件响应。
 */
public record AttachmentVO(
        long id,
        String publicUrl,
        String contentType,
        long fileSize,
        int width,
        int height,
        String originalFilename,
        LocalDateTime createdAt,
        Long createdBy
) {
    public static AttachmentVO from(AttachmentResult result) {
        return new AttachmentVO(
                result.id(),
                result.publicUrl(),
                result.contentType(),
                result.fileSize(),
                result.width(),
                result.height(),
                result.originalFilename(),
                result.createdAt(),
                result.createdBy());
    }
}
