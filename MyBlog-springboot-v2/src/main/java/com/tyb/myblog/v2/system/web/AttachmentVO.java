package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.system.application.attachment.AttachmentResult;

import java.time.LocalDateTime;

/**
 * 后台附件响应。
 */
public record AttachmentVO(
        long id,
        StorageType storageType,
        String bucket,
        String objectKey,
        String publicUrl,
        String contentType,
        long fileSize,
        int width,
        int height,
        String originalFilename,
        String hashSha256,
        LocalDateTime createdAt,
        Long createdBy
) {
    public static AttachmentVO from(AttachmentResult result) {
        return new AttachmentVO(
                result.id(),
                result.storageType(),
                result.bucket(),
                result.objectKey(),
                result.publicUrl(),
                result.contentType(),
                result.fileSize(),
                result.width(),
                result.height(),
                result.originalFilename(),
                result.hashSha256(),
                result.createdAt(),
                result.createdBy());
    }
}
