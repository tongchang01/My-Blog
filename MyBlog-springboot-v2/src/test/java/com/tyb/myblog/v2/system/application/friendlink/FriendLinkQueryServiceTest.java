package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLink;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkPage;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkRepository;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendLinkQueryServiceTest {

    @Mock
    private FriendLinkRepository repository;

    private FriendLinkQueryService service;

    @BeforeEach
    void setUp() {
        service = new FriendLinkQueryService(
                repository, new FriendLinkAuthorization());
    }

    @Test
    void returnsPublicVisibleResultsWithoutAuditFields() {
        when(repository.findPublicVisible())
                .thenReturn(List.of(friendLink(
                        FriendLinkStatus.VISIBLE)));

        assertThat(service.publicList())
                .singleElement()
                .extracting(PublicFriendLinkResult::name)
                .isEqualTo("Example");
    }

    @Test
    void allowsAdminAndDemoToReadAdminPage() {
        when(repository.findActivePage(1, 20))
                .thenReturn(new FriendLinkPage(
                        List.of(friendLink(FriendLinkStatus.HIDDEN)),
                        1, 1, 20));

        assertThat(service.adminPage(
                principal("ADMIN"), 1, 20).total())
                .isEqualTo(1);
        assertThat(service.adminPage(
                principal("DEMO"), 1, 20).total())
                .isEqualTo(1);
    }

    @Test
    void rejectsMissingAndUnreadablePrincipal() {
        assertError(
                () -> service.adminPage(null, 1, 20),
                ApiErrorCode.INVALID_TOKEN);
        assertError(
                () -> service.adminPage(
                        principal("GUEST"), 1, 20),
                ApiErrorCode.FORBIDDEN);
    }

    @Test
    void validatesPageAndIdentityBoundaries() {
        assertError(
                () -> service.adminPage(
                        principal("ADMIN"), 0, 20),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.adminPage(
                        principal("ADMIN"), 1, 0),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.adminPage(
                        principal("ADMIN"), 1, 101),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.adminDetail(
                        principal("ADMIN"), 0),
                ApiErrorCode.VALIDATION_ERROR);
    }

    @Test
    void returnsAdminDetailAndMapsMissingToNotFound() {
        when(repository.findActiveById(10L))
                .thenReturn(Optional.of(friendLink(
                        FriendLinkStatus.VISIBLE)));
        when(repository.findActiveById(11L))
                .thenReturn(Optional.empty());

        assertThat(service.adminDetail(
                principal("DEMO"), 10L).id()).isEqualTo(10L);
        assertError(
                () -> service.adminDetail(
                        principal("ADMIN"), 11L),
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

    private FriendLink friendLink(FriendLinkStatus status) {
        return FriendLink.reconstitute(
                10L,
                "Example",
                "https://example.com",
                "https://example.com/logo.png",
                "介绍",
                10,
                status,
                LocalDateTime.of(2026, 6, 14, 12, 0),
                1001L,
                LocalDateTime.of(2026, 6, 14, 12, 30),
                1001L);
    }
}
