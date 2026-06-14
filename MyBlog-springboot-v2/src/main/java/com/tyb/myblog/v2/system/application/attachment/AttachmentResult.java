package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.system.domain.attachment.Attachment;

import java.time.LocalDateTime;

/**
 * 后台附件公开结果。
 */
public record AttachmentResult(
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
    public static AttachmentResult from(Attachment attachment) {
        return new AttachmentResult(
                attachment.id(),
                attachment.storageType(),
                attachment.bucket(),
                attachment.objectKey(),
                attachment.publicUrl(),
                attachment.contentType(),
                attachment.fileSize(),
                attachment.width(),
                attachment.height(),
                attachment.originalFilename(),
                attachment.hashSha256(),
                attachment.createdAt(),
                attachment.createdBy());
    }
}
