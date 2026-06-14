package com.tyb.myblog.v2.common.infrastructure.storage.local;

import com.tyb.myblog.v2.common.storage.StoreObjectCommand;
import com.tyb.myblog.v2.common.storage.StorageOperationException;
import com.tyb.myblog.v2.common.storage.StorageService;
import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.common.storage.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * 本地文件系统附件存储。
 */
public class LocalStorageService implements StorageService {

    private static final Logger log =
            LoggerFactory.getLogger(LocalStorageService.class);

    private final Path root;
    private final String bucketAlias;
    private final URI publicBaseUrl;

    public LocalStorageService(
            Path root,
            String bucketAlias,
            URI publicBaseUrl) {
        try {
            this.root = root.toAbsolutePath().normalize();
            this.bucketAlias = bucketAlias;
            this.publicBaseUrl = publicBaseUrl;
            Files.createDirectories(this.root);
            if (!Files.isDirectory(this.root)
                    || !Files.isWritable(this.root)) {
                throw new StorageOperationException(
                        "LOCAL 附件目录不可写");
            }
        } catch (IOException exception) {
            throw new StorageOperationException(
                    "LOCAL 附件目录初始化失败", exception);
        }
    }

    @Override
    public StorageType type() {
        return StorageType.LOCAL;
    }

    @Override
    public StoredObject store(StoreObjectCommand command) {
        Path target = resolve(command.objectKey());
        Path temp = null;
        try {
            Files.createDirectories(target.getParent());
            temp = Files.createTempFile(
                    target.getParent(), ".upload-", ".tmp");
            Files.copy(command.source(), temp, REPLACE_EXISTING);
            move(temp, target);
            return new StoredObject(
                    type(),
                    bucketAlias,
                    command.objectKey(),
                    publicBaseUrl + "/" + command.objectKey());
        } catch (IOException exception) {
            deleteQuietly(temp);
            throw new StorageOperationException(
                    "LOCAL 附件写入失败", exception);
        }
    }

    @Override
    public boolean exists(String bucket, String objectKey) {
        requireBucket(bucket);
        return Files.isRegularFile(resolve(objectKey));
    }

    @Override
    public void delete(String bucket, String objectKey) {
        requireBucket(bucket);
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException exception) {
            throw new StorageOperationException(
                    "LOCAL 附件删除失败", exception);
        }
    }

    private void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            log.warn("当前文件系统不支持原子移动，objectKey={}",
                    root.relativize(target));
            Files.move(source, target, REPLACE_EXISTING);
        }
    }

    private Path resolve(String objectKey) {
        if (objectKey == null
                || objectKey.isBlank()
                || objectKey.contains("\\")
                || objectKey.matches("^[A-Za-z]:/.*")
                || objectKey.startsWith("/")) {
            throw new StorageOperationException("附件对象键格式错误");
        }
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new StorageOperationException("附件对象键越出存储目录");
        }
        return target;
    }

    private void requireBucket(String bucket) {
        if (!bucketAlias.equals(bucket)) {
            throw new StorageOperationException("LOCAL 存储桶别名不匹配");
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("LOCAL 临时文件清理失败，path={}", path);
        }
    }
}
