package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.system.domain.attachment.Attachment;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * content 使用的附件引用校验和批量 URL 解析测试。
 */
@ExtendWith(MockitoExtension.class)
class AttachmentReferenceServiceTest {

    @Mock
    private AttachmentRepository repository;

    private AttachmentReferenceService service;

    @BeforeEach
    void setUp() {
        service = new AttachmentReferenceService(repository);
    }

    @Test
    void locksAndReturnsActiveImage() {
        when(repository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.of(attachment(
                        10L,
                        "image/png",
                        "http://localhost/media/a.png")));

        AttachmentReferenceResult result =
                service.requireActiveImageForUpdate(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.publicUrl())
                .isEqualTo("http://localhost/media/a.png");
        assertThat(result.contentType()).isEqualTo("image/png");
    }

    @Test
    void rejectsMissingAndNonImageAttachment() {
        when(repository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.empty());
        when(repository.findActiveByIdForUpdate(20L))
                .thenReturn(Optional.of(attachment(
                        20L,
                        "text/plain",
                        "http://localhost/media/a.txt")));

        assertThatThrownBy(() ->
                service.requireActiveImageForUpdate(10L))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.NOT_FOUND);
        assertThatThrownBy(() ->
                service.requireActiveImageForUpdate(20L))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.CONFLICT);
    }

    @Test
    void resolvesPublicUrlsInOneSortedBatch() {
        when(repository.findActiveByIds(List.of(10L, 20L)))
                .thenReturn(List.of(
                        attachment(
                                10L,
                                "image/png",
                                "http://localhost/media/a.png"),
                        attachment(
                                20L,
                                "image/jpeg",
                                "http://localhost/media/b.jpg")));

        Map<Long, String> result =
                service.resolvePublicUrls(Set.of(20L, 10L));

        assertThat(result)
                .containsEntry(10L, "http://localhost/media/a.png")
                .containsEntry(20L, "http://localhost/media/b.jpg");
        verify(repository).findActiveByIds(List.of(10L, 20L));
    }

    @Test
    void ignoresNullEmptyAndInvalidBatchIds() {
        assertThat(service.resolvePublicUrls(null)).isEmpty();
        assertThat(service.resolvePublicUrls(Set.of())).isEmpty();
        assertThat(service.resolvePublicUrls(
                new java.util.HashSet<>(
                        java.util.Arrays.asList(null, -1L, 0L))))
                .isEmpty();
    }

    private Attachment attachment(
            long id,
            String contentType,
            String publicUrl) {
        return Attachment.reconstitute(
                id,
                StorageType.LOCAL,
                "local",
                "attachments/2026/06/" + id,
                publicUrl,
                contentType,
                100,
                10,
                10,
                "file-" + id,
                "a".repeat(64),
                LocalDateTime.of(2026, 6, 15, 20, 0),
                1001L);
    }
}
