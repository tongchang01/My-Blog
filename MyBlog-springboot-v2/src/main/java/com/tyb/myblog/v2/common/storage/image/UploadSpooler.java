package com.tyb.myblog.v2.common.storage.image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * 将上传流落到临时文件，同时限制大小并计算摘要。
 */
public class UploadSpooler {

    private final Path tempDirectory;

    public UploadSpooler(Path tempDirectory) {
        this.tempDirectory = tempDirectory.toAbsolutePath().normalize();
    }

    public SpooledUpload spool(InputStream input, long maxBytes)
            throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        Files.createDirectories(tempDirectory);
        Path temp = Files.createTempFile(
                tempDirectory, "myblog-upload-", ".tmp");
        MessageDigest digest = sha256();
        long size = 0;
        try (var output = Files.newOutputStream(
                temp, WRITE, TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                size += read;
                if (size > maxBytes) {
                    throw new IllegalArgumentException(
                            "上传文件不能超过10 MiB");
                }
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
            if (size == 0) {
                throw new IllegalArgumentException("上传文件不能为空");
            }
            return new SpooledUpload(
                    temp,
                    size,
                    HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(temp);
            throw exception;
        }
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM 不支持 SHA-256", exception);
        }
    }
}
