package com.tyb.myblog.v2.system.domain.attachment;

import com.tyb.myblog.v2.common.storage.StorageType;

import java.time.LocalDateTime;

/**
 * 可供后台使用的附件领域对象。
 */
public record Attachment(
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

    /**
     * 从持久化数据重建附件。
     */
    public static Attachment reconstitute(
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
            Long createdBy) {
        if (id <= 0) {
            throw new IllegalArgumentException("附件 ID 必须为正数");
        }
        AttachmentValidation.Values values = AttachmentValidation.validate(
                storageType, bucket, objectKey, publicUrl, contentType,
                fileSize, width, height, originalFilename, hashSha256);
        return new Attachment(
                id,
                values.storageType(),
                values.bucket(),
                values.objectKey(),
                values.publicUrl(),
                values.contentType(),
                values.fileSize(),
                values.width(),
                values.height(),
                values.originalFilename(),
                values.hashSha256(),
                createdAt,
                createdBy);
    }
}
