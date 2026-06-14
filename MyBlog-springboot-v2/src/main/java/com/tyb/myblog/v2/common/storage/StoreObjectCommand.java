package com.tyb.myblog.v2.common.storage;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 将受控临时文件写入物理存储的命令。
 */
public record StoreObjectCommand(
        Path source,
        String objectKey,
        String contentType,
        long contentLength
) {
    public StoreObjectCommand {
        Objects.requireNonNull(source, "存储源文件不能为空");
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("对象键不能为空");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("文件类型不能为空");
        }
        if (contentLength <= 0) {
            throw new IllegalArgumentException("文件长度必须为正数");
        }
    }
}
