package com.tyb.myblog.v2.common.storage;

import com.tyb.myblog.v2.common.infrastructure.storage.s3.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3StorageServiceTest {

    @TempDir
    Path tempDir;

    private S3Client client;
    private S3StorageService storage;

    @BeforeEach
    void setUp() {
        client = mock(S3Client.class);
        storage = new S3StorageService(
                client,
                "myblog-assets",
                URI.create("https://static.example.com"));
    }

    @Test
    void uploadsWithExactMetadataAndWithoutObjectAcl() throws Exception {
        Path source = tempDir.resolve("a.webp");
        Files.write(source, new byte[]{1, 2, 3});

        StoredObject result = storage.store(new StoreObjectCommand(
                source,
                "attachments/2026/06/a.webp",
                "image/webp",
                3L));

        ArgumentCaptor<PutObjectRequest> request =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("myblog-assets");
        assertThat(request.getValue().key())
                .isEqualTo("attachments/2026/06/a.webp");
        assertThat(request.getValue().contentType()).isEqualTo("image/webp");
        assertThat(request.getValue().contentLength()).isEqualTo(3L);
        assertThat(request.getValue().acl()).isNull();
        assertThat(result.publicUrl()).isEqualTo(
                "https://static.example.com/attachments/2026/06/a.webp");
    }

    @Test
    void checksExistenceAndMapsNotFound() {
        assertThat(storage.exists(
                "myblog-assets", "attachments/a.webp")).isTrue();

        when(client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder()
                        .statusCode(404)
                        .message("missing")
                        .build());

        assertThat(storage.exists(
                "myblog-assets", "attachments/missing.webp")).isFalse();
    }

    @Test
    void deletesWithConfiguredBucketAndMapsUnexpectedSdkFailure() {
        storage.delete("myblog-assets", "attachments/a.webp");
        verify(client).deleteObject(any(DeleteObjectRequest.class));

        when(client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .statusCode(503)
                        .message("unavailable")
                        .build());

        assertThatThrownBy(() -> storage.exists(
                "myblog-assets", "attachments/a.webp"))
                .isInstanceOf(StorageOperationException.class)
                .hasMessage("附件存储操作失败");
    }

    @Test
    void rejectsBucketThatDoesNotMatchConfiguration() {
        assertThatThrownBy(() -> storage.exists(
                "other", "attachments/a.webp"))
                .isInstanceOf(StorageOperationException.class);
    }
}
