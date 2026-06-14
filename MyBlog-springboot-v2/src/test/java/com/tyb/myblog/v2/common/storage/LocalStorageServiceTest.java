package com.tyb.myblog.v2.common.storage;

import com.tyb.myblog.v2.common.infrastructure.storage.local.LocalStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesChecksAndDeletesObject() throws Exception {
        Path source = tempDir.resolve("source.tmp");
        Files.writeString(source, "content");
        Path root = tempDir.resolve("root");
        LocalStorageService storage = new LocalStorageService(
                root, "local", URI.create("http://localhost:8080/media"));

        StoredObject result = storage.store(new StoreObjectCommand(
                source,
                "attachments/2026/06/a.png",
                "image/png",
                Files.size(source)));

        assertThat(result.storageType()).isEqualTo(StorageType.LOCAL);
        assertThat(result.publicUrl()).isEqualTo(
                "http://localhost:8080/media/attachments/2026/06/a.png");
        assertThat(storage.exists("local", result.objectKey())).isTrue();
        assertThat(Files.readString(
                root.resolve(result.objectKey()))).isEqualTo("content");

        storage.delete("local", result.objectKey());
        assertThat(storage.exists("local", result.objectKey())).isFalse();
    }

    @Test
    void rejectsWrongBucketAndPathTraversal() {
        LocalStorageService storage = new LocalStorageService(
                tempDir.resolve("root"),
                "local",
                URI.create("http://localhost/media"));

        assertThatThrownBy(() -> storage.exists("wrong", "a.png"))
                .isInstanceOf(StorageOperationException.class);
        assertThatThrownBy(() -> storage.exists("local", "../a.png"))
                .isInstanceOf(StorageOperationException.class);
        assertThatThrownBy(() -> storage.exists("local", "C:/a.png"))
                .isInstanceOf(StorageOperationException.class);
    }
}
