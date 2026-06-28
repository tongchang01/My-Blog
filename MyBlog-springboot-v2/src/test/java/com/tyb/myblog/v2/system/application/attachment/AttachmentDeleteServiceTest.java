package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.system.domain.attachment.Attachment;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachmentDeleteServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-28T02:00:00Z"),
            ZoneId.of("Asia/Tokyo"));

    private final AttachmentRepository repository =
            mock(AttachmentRepository.class);

    private final AttachmentDeleteService service =
            new AttachmentDeleteService(repository, CLOCK);

    @Test
    void softDeletesActiveAttachmentForAdmin() {
        when(repository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.of(attachment()));
        when(repository.softDelete(
                10L,
                LocalDateTime.of(2026, 6, 28, 11, 0),
                1001L))
                .thenReturn(true);

        service.delete(principal("1001", "ADMIN"), 10L);

        verify(repository).softDelete(
                10L,
                LocalDateTime.of(2026, 6, 28, 11, 0),
                1001L);
    }

    @Test
    void rejectsDemoAndInvalidId() {
        assertError(
                () -> service.delete(principal("1002", "DEMO"), 10L),
                ApiErrorCode.FORBIDDEN);
        assertError(
                () -> service.delete(principal("1001", "ADMIN"), 0L),
                ApiErrorCode.VALIDATION_ERROR);
    }

    private void assertError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            ApiErrorCode code) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(code));
    }

    private AuthenticatedPrincipal principal(String id, String role) {
        return new AuthenticatedPrincipal(
                id, role.toLowerCase(), List.of(role));
    }

    private Attachment attachment() {
        return Attachment.reconstitute(
                10L,
                StorageType.LOCAL,
                "local",
                "attachments/2026/06/a.png",
                "http://localhost/media/attachments/2026/06/a.png",
                "image/png",
                128L,
                2,
                3,
                "a.png",
                "a".repeat(64),
                LocalDateTime.of(2026, 6, 14, 12, 0),
                1001L);
    }
}
