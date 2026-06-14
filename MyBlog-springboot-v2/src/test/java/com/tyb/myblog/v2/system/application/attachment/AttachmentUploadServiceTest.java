package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.common.storage.StorageService;
import com.tyb.myblog.v2.common.storage.StorageServiceRegistry;
import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.common.storage.StoredObject;
import com.tyb.myblog.v2.common.storage.config.StorageProperties;
import com.tyb.myblog.v2.common.storage.image.ImageFormat;
import com.tyb.myblog.v2.common.storage.image.ImageInspector;
import com.tyb.myblog.v2.common.storage.image.InspectedImage;
import com.tyb.myblog.v2.common.storage.image.SpooledUpload;
import com.tyb.myblog.v2.common.storage.image.UploadSpooler;
import com.tyb.myblog.v2.system.domain.attachment.Attachment;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentLookup;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DuplicateKeyException;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachmentUploadServiceTest {

    @TempDir
    Path tempDir;

    private final AttachmentRepository repository =
            mock(AttachmentRepository.class);
    private final StorageServiceRegistry registry =
            mock(StorageServiceRegistry.class);
    private final UploadSpooler spooler = mock(UploadSpooler.class);
    private final ImageInspector inspector = mock(ImageInspector.class);
    private final AttachmentRegistrationService registration =
            mock(AttachmentRegistrationService.class);
    private final AttachmentRestoreService restore =
            mock(AttachmentRestoreService.class);
    private final ObjectKeyGenerator keyGenerator =
            mock(ObjectKeyGenerator.class);
    private final StorageService storage = mock(StorageService.class);
    private final StorageProperties properties = new StorageProperties();
    private AttachmentUploadService service;
    private Path spooledPath;

    @BeforeEach
    void setUp() throws Exception {
        spooledPath = tempDir.resolve("upload.tmp");
        Files.write(spooledPath, new byte[]{1, 2, 3});
        when(spooler.spool(any(), anyLong()))
                .thenReturn(new SpooledUpload(
                        spooledPath, 3L, "a".repeat(64)));
        when(inspector.inspect(spooledPath))
                .thenReturn(new InspectedImage(ImageFormat.PNG, 2, 3));
        properties.setMaxFileSize(
                org.springframework.util.unit.DataSize.ofMegabytes(10));
        service = new AttachmentUploadService(
                repository, registry, spooler, inspector,
                registration, restore, keyGenerator, properties);
    }

    @Test
    void rejectsMissingOrNonAdminPrincipal() {
        assertCode(
                () -> service.upload(null, command()),
                ApiErrorCode.INVALID_TOKEN);
        assertCode(
                () -> service.upload(
                        principal("1002", "DEMO"), command()),
                ApiErrorCode.FORBIDDEN);
        assertCode(
                () -> service.upload(
                        principal("invalid", "ADMIN"), command()),
                ApiErrorCode.INVALID_TOKEN);
    }

    @Test
    void reusesActiveAttachmentWhenPhysicalObjectExists()
            throws Exception {
        Attachment active = attachment();
        when(repository.findByHashIncludingDeleted("a".repeat(64)))
                .thenReturn(Optional.of(
                        new AttachmentLookup(active, false)));
        when(registry.required(StorageType.LOCAL)).thenReturn(storage);
        when(storage.exists(active.bucket(), active.objectKey()))
                .thenReturn(true);

        AttachmentResult result = service.upload(
                principal("1001", "ADMIN"), command());

        assertThat(result.id()).isEqualTo(active.id());
        verify(storage, never()).store(any());
        verify(registration, never()).register(any());
        assertThat(spooledPath).doesNotExist();
    }

    @Test
    void restoresDeletedAttachmentWithoutWritingNewObject()
            throws Exception {
        Attachment deleted = attachment();
        Attachment active = attachment();
        AttachmentLookup lookup = new AttachmentLookup(deleted, true);
        when(repository.findByHashIncludingDeleted("a".repeat(64)))
                .thenReturn(Optional.of(lookup));
        when(registry.required(StorageType.LOCAL)).thenReturn(storage);
        when(storage.exists(deleted.bucket(), deleted.objectKey()))
                .thenReturn(true);
        when(restore.restore(lookup, 1001L)).thenReturn(active);

        assertThat(service.upload(
                principal("1001", "ADMIN"), command()).id())
                .isEqualTo(active.id());
        verify(restore).restore(lookup, 1001L);
        verify(storage, never()).store(any());
    }

    @Test
    void storesAndRegistersNewAttachment() throws Exception {
        when(repository.findByHashIncludingDeleted("a".repeat(64)))
                .thenReturn(Optional.empty());
        when(registry.current()).thenReturn(storage);
        when(keyGenerator.generate(ImageFormat.PNG))
                .thenReturn("attachments/2026/06/new.png");
        when(storage.store(any())).thenReturn(new StoredObject(
                StorageType.LOCAL, "local",
                "attachments/2026/06/new.png",
                "http://localhost/media/attachments/2026/06/new.png"));
        when(registration.register(any())).thenReturn(attachment());

        AttachmentResult result = service.upload(
                principal("1001", "ADMIN"), command());

        assertThat(result.id()).isEqualTo(10L);
        verify(storage).store(any());
        verify(registration).register(any());
    }

    @Test
    void compensatesObjectAndReturnsWinnerOnHashRace()
            throws Exception {
        Attachment winner = attachment();
        when(repository.findByHashIncludingDeleted("a".repeat(64)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(
                        new AttachmentLookup(winner, false)));
        when(registry.current()).thenReturn(storage);
        when(registry.required(StorageType.LOCAL)).thenReturn(storage);
        when(storage.exists(winner.bucket(), winner.objectKey()))
                .thenReturn(true);
        when(keyGenerator.generate(ImageFormat.PNG))
                .thenReturn("attachments/2026/06/new.png");
        StoredObject object = new StoredObject(
                StorageType.LOCAL, "local",
                "attachments/2026/06/new.png",
                "http://localhost/media/attachments/2026/06/new.png");
        when(storage.store(any())).thenReturn(object);
        when(registration.register(any()))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertThat(service.upload(
                principal("1001", "ADMIN"), command()).id())
                .isEqualTo(winner.id());
        verify(storage).delete(object.bucket(), object.objectKey());
    }

    private AttachmentUploadCommand command() {
        return new AttachmentUploadCommand(
                "cover.png",
                new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }

    private AuthenticatedPrincipal principal(String id, String role) {
        return new AuthenticatedPrincipal(id, "user", List.of(role));
    }

    private Attachment attachment() {
        return Attachment.reconstitute(
                10L, StorageType.LOCAL, "local",
                "attachments/2026/06/a.png",
                "http://localhost/media/attachments/2026/06/a.png",
                "image/png", 3L, 2, 3, "cover.png",
                "a".repeat(64),
                LocalDateTime.of(2026, 6, 14, 12, 0),
                1001L);
    }

    private void assertCode(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
            ApiErrorCode code) {
        assertThatThrownBy(action)
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(
                        ((ApiException) exception).code()).isEqualTo(code));
    }
}
