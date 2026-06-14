package com.tyb.myblog.v2.common.storage;

/**
 * 附件物理存储端口。
 */
public interface StorageService {

    StorageType type();

    StoredObject store(StoreObjectCommand command);

    boolean exists(String bucket, String objectKey);

    void delete(String bucket, String objectKey);
}
