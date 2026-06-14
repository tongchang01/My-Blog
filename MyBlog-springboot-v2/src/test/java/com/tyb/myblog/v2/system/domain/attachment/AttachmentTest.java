package com.tyb.myblog.v2.system.domain.attachment;

import com.tyb.myblog.v2.common.storage.StorageType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttachmentTest {

    private static final String HASH = "a".repeat(64);

    @Test
    void createsValidatedAttachment() {
        Attachment attachment = attachment(
                1001L, StorageType.S3, "cover.webp", HASH);

        assertThat(attachment.id()).isEqualTo(1001L);
        assertThat(attachment.storageType()).isEqualTo(StorageType.S3);
        assertThat(attachment.originalFilename()).isEqualTo("cover.webp");
    }

    @Test
    void acceptsHistoricalOssMetadata() {
        assertThat(attachment(1001L, StorageType.OSS, null, HASH)
                .storageType()).isEqualTo(StorageType.OSS);
    }

    @Test
    void rejectsInvalidIdentityAndStorageMetadata() {
        assertThatThrownBy(() -> attachment(0L, StorageType.S3, null, HASH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Attachment.reconstitute(
                1L, StorageType.S3, "bucket", "../a.webp",
                "https://static.example.com/a.webp", "image/webp",
                10L, 1, 1, null, HASH, LocalDateTime.now(), 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Attachment.reconstitute(
                1L, StorageType.S3, "bucket", "a.webp",
                "/a.webp", "image/webp",
                10L, 1, 1, null, HASH, LocalDateTime.now(), 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidHashFileSizeAndDimensions() {
        assertThatThrownBy(() -> attachment(1L, StorageType.S3, null, "A".repeat(64)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Attachment.reconstitute(
                1L, StorageType.S3, "bucket", "a.webp",
                "https://static.example.com/a.webp", "image/webp",
                0L, 1, 1, null, HASH, LocalDateTime.now(), 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Attachment.reconstitute(
                1L, StorageType.S3, "bucket", "a.webp",
                "https://static.example.com/a.webp", "image/webp",
                10L, 20_001, 1, null, HASH, LocalDateTime.now(), 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Attachment.reconstitute(
                1L, StorageType.S3, "bucket", "a.webp",
                "https://static.example.com/a.webp", "image/webp",
                10L, 10_000, 10_000, null, HASH, LocalDateTime.now(), 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sanitizesOriginalFilenameAndSharesRulesWithNewAttachment() {
        Attachment attachment = attachment(
                1L, StorageType.LOCAL, " C:\\fake\\cover.png\u0000 ", HASH);
        NewAttachment created = NewAttachment.create(
                StorageType.LOCAL, "local", "attachments/2026/06/a.png",
                "http://localhost:8080/media/attachments/2026/06/a.png",
                "image/png", 10L, 2, 3,
                " ../cover.png ", HASH, 1001L);

        assertThat(attachment.originalFilename()).isEqualTo("cover.png");
        assertThat(created.originalFilename()).isEqualTo("cover.png");
    }

    private Attachment attachment(
            long id,
            StorageType storageType,
            String originalFilename,
            String hash) {
        return Attachment.reconstitute(
                id,
                storageType,
                "myblog-assets",
                "attachments/2026/06/a.webp",
                "https://static.example.com/attachments/2026/06/a.webp",
                "image/webp",
                1024L,
                1600,
                900,
                originalFilename,
                hash,
                LocalDateTime.of(2026, 6, 14, 12, 0),
                2001L);
    }
}
