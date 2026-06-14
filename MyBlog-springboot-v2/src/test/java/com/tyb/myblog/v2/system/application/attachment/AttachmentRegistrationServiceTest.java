package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.system.domain.attachment.Attachment;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentLookup;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import com.tyb.myblog.v2.system.domain.attachment.NewAttachment;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachmentRegistrationServiceTest {

    private final AttachmentRepository repository =
            mock(AttachmentRepository.class);

    @Test
    void registersNewAttachmentThroughRepository() {
        NewAttachment created = newAttachment();
        Attachment stored = attachment(false);
        when(repository.insert(created)).thenReturn(stored);
        AttachmentRegistrationService service =
                new AttachmentRegistrationService(repository);

        assertThat(service.register(created)).isSameAs(stored);
        verify(repository).insert(created);
    }

    @Test
    void restoresDeletedAttachmentWithFixedAuditTime() {
        Attachment deleted = attachment(true);
        Attachment active = attachment(false);
        when(repository.restoreDeleted(
                10L,
                LocalDateTime.of(2026, 6, 14, 12, 0),
                1001L)).thenReturn(true);
        when(repository.findByHashIncludingDeleted(deleted.hashSha256()))
                .thenReturn(Optional.of(
                        new AttachmentLookup(active, false)));
        AttachmentRestoreService service = new AttachmentRestoreService(
                repository,
                Clock.fixed(
                        Instant.parse("2026-06-14T03:00:00Z"),
                        ZoneId.of("Asia/Tokyo")));

        assertThat(service.restore(
                new AttachmentLookup(deleted, true), 1001L))
                .isSameAs(active);
    }

    @Test
    void failsWhenRestoreCannotProduceActiveRecord() {
        Attachment deleted = attachment(true);
        when(repository.findByHashIncludingDeleted(deleted.hashSha256()))
                .thenReturn(Optional.of(
                        new AttachmentLookup(deleted, true)));
        AttachmentRestoreService service = new AttachmentRestoreService(
                repository,
                Clock.system(ZoneId.of("Asia/Tokyo")));

        assertThatThrownBy(() -> service.restore(
                new AttachmentLookup(deleted, true), 1001L))
                .isInstanceOf(ApiException.class);
    }

    private NewAttachment newAttachment() {
        return NewAttachment.create(
                StorageType.LOCAL, "local",
                "attachments/2026/06/a.png",
                "http://localhost/media/attachments/2026/06/a.png",
                "image/png", 10L, 2, 3, "a.png",
                "a".repeat(64), 1001L);
    }

    private Attachment attachment(boolean ignoredDeletedState) {
        return Attachment.reconstitute(
                10L, StorageType.LOCAL, "local",
                "attachments/2026/06/a.png",
                "http://localhost/media/attachments/2026/06/a.png",
                "image/png", 10L, 2, 3, "a.png",
                "a".repeat(64),
                LocalDateTime.of(2026, 6, 14, 12, 0),
                1001L);
    }
}
