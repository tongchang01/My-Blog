package com.tyb.myblog.v2.common.storage.config;

import com.tyb.myblog.v2.common.storage.StorageType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.net.URI;
import java.nio.file.Path;

/**
 * 附件存储运行配置。
 */
@Getter
@Setter
@ConfigurationProperties("myblog.storage")
public class StorageProperties {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private StorageType type = StorageType.LOCAL;
    private DataSize maxFileSize = DataSize.ofMegabytes(10);
    private Local local = new Local();
    private S3 s3 = new S3();

    public long getMaxFileBytes() {
        return maxFileSize.toBytes();
    }

    /**
     * 校验当前上传后端并规范化公开地址。
     */
    public void validate() {
        long bytes = getMaxFileBytes();
        if (bytes <= 0 || bytes > MAX_FILE_BYTES) {
            throw new IllegalStateException(
                    "附件最大大小必须在1字节到10 MiB之间");
        }
        if (type == StorageType.OSS) {
            throw new IllegalStateException("当前版本不支持 OSS 上传");
        }
        if (type == StorageType.LOCAL) {
            validateLocal();
        } else if (type == StorageType.S3) {
            validateS3();
        }
    }

    public boolean hasCompleteS3() {
        return text(s3.region)
                && text(s3.bucket)
                && validHttpUrl(s3.publicBaseUrl);
    }

    private void validateLocal() {
        if (local.root == null
                || !text(local.bucketAlias)
                || !validHttpUrl(local.publicBaseUrl)) {
            throw new IllegalStateException("LOCAL 附件存储配置不完整");
        }
        local.publicBaseUrl = normalize(local.publicBaseUrl);
    }

    private void validateS3() {
        if (!hasCompleteS3()) {
            throw new IllegalStateException("S3 附件存储配置不完整");
        }
        s3.publicBaseUrl = normalize(s3.publicBaseUrl);
    }

    private boolean validHttpUrl(URI value) {
        return value != null
                && value.getHost() != null
                && ("http".equalsIgnoreCase(value.getScheme())
                || "https".equalsIgnoreCase(value.getScheme()));
    }

    private URI normalize(URI value) {
        String normalized = value.toString();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return URI.create(normalized);
    }

    private boolean text(String value) {
        return value != null && !value.isBlank();
    }

    @Getter
    @Setter
    public static class Local {
        private Path root;
        private String bucketAlias;
        private URI publicBaseUrl;
        private Boolean webEnabled;
    }

    @Getter
    @Setter
    public static class S3 {
        private String region;
        private String bucket;
        private URI publicBaseUrl;
    }
}
