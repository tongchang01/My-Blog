package com.tyb.myblog.v2.common.storage;

/**
 * 物理存储操作失败。
 */
public class StorageOperationException extends RuntimeException {

    public StorageOperationException(String message) {
        super(message);
    }

    public StorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
