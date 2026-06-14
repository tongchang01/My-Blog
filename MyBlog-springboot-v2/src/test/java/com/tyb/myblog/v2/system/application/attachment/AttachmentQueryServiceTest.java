package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.system.domain.attachment.Attachment;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentPage;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentQueryServiceTest {

    @Mock
    private AttachmentRepository repository;

    @InjectMocks
    private AttachmentQueryService service;

    @Test
    void allowsAdminAndDemoToPageActiveAttachments() {
        when(repository.findActivePage(1, 20))
                .thenReturn(new AttachmentPage(
                        List.of(attachment()), 1, 1, 20));

        var adminResult = service.page(principal("ADMIN"), 1, 20);
        var demoResult = service.page(principal("DEMO"), 1, 20);

        assertThat(adminResult.records()).hasSize(1);
        assertThat(demoResult.records()).hasSize(1);
        assertThat(adminResult.total()).isEqualTo(1);
        verify(repository, org.mockito.Mockito.times(2))
                .findActivePage(1, 20);
    }

    @Test
    void rejectsMissingOrUnreadablePrincipal() {
        assertError(
                () -> service.page(null, 1, 20),
                ApiErrorCode.INVALID_TOKEN);
        assertError(
                () -> service.page(principal("GUEST"), 1, 20),
                ApiErrorCode.FORBIDDEN);
    }

    @Test
    void validatesPageBoundaries() {
        assertError(
                () -> service.page(principal("ADMIN"), 0, 20),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.page(principal("ADMIN"), 1, 0),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.page(principal("ADMIN"), 1, 101),
                ApiErrorCode.VALIDATION_ERROR);
    }

    @Test
    void returnsActiveDetailAndMapsMissingToNotFound() {
        when(repository.findActiveById(10L))
                .thenReturn(Optional.of(attachment()));
        when(repository.findActiveById(11L))
                .thenReturn(Optional.empty());

        assertThat(service.detail(principal("DEMO"), 10L).id())
                .isEqualTo(10L);
        assertError(
                () -> service.detail(principal("ADMIN"), 0L),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.detail(principal("ADMIN"), 11L),
                ApiErrorCode.NOT_FOUND);
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

    private AuthenticatedPrincipal principal(String role) {
        return new AuthenticatedPrincipal(
                "1001", role.toLowerCase(), List.of(role));
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
