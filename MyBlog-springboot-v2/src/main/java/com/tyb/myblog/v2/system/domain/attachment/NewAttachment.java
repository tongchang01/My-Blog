package com.tyb.myblog.v2.system.domain.attachment;

import com.tyb.myblog.v2.common.storage.StorageType;

/**
 * 等待登记的新附件。
 */
public record NewAttachment(
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
        long createdBy
) {

    /**
     * 创建经过领域校验的新附件。
     */
    public static NewAttachment create(
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
            long createdBy) {
        if (createdBy <= 0) {
            throw new IllegalArgumentException("附件创建人 ID 必须为正数");
        }
        AttachmentValidation.Values values = AttachmentValidation.validate(
                storageType, bucket, objectKey, publicUrl, contentType,
                fileSize, width, height, originalFilename, hashSha256);
        return new NewAttachment(
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
                createdBy);
    }
}
