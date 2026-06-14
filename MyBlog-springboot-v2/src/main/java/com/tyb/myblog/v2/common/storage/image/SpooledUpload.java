package com.tyb.myblog.v2.common.storage.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 受控临时上传文件，关闭时删除。
 */
public record SpooledUpload(
        Path path,
        long size,
        String sha256
) implements AutoCloseable {

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(path);
    }
}
