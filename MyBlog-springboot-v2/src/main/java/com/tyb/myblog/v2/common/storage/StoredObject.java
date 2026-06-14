package com.tyb.myblog.v2.common.storage;

/**
 * 已写入物理存储的对象信息。
 */
public record StoredObject(
        StorageType storageType,
        String bucket,
        String objectKey,
        String publicUrl
) {
}
