package com.tyb.myblog.v2.common.storage;

/**
 * 附件物理存储类型。
 */
public enum StorageType {
    LOCAL,
    S3,
    OSS;

    /**
     * 从数据库稳定值解析存储类型。
     */
    public static StorageType parse(String value) {
        try {
            return StorageType.valueOf(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("不支持的附件存储类型", exception);
        }
    }
}
