package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.application.token.UserTokenRevocationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 全端退出应用服务单元测试。
 */
class LogoutApplicationServiceTest {

    private final UserTokenRevocationService revocationService =
            mock(UserTokenRevocationService.class);
    private final LogoutApplicationService service =
            new LogoutApplicationService(revocationService);

    @Test
    void shouldRevokeCurrentAccountTokens() {
        when(revocationService.revokeAll(1001L, 1001L))
                .thenReturn(true);

        service.logout("1001");

        verify(revocationService).revokeAll(1001L, 1001L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "abc", "0", "-1"})
    void shouldRejectInvalidPrincipalId(String principalId) {
        assertInvalidToken(() -> service.logout(principalId));
        verifyNoInteractions(revocationService);
    }

    @Test
    void shouldMapMissingAccountToInvalidToken() {
        when(revocationService.revokeAll(1001L, 1001L))
                .thenReturn(false);

        assertInvalidToken(() -> service.logout("1001"));
    }

    private void assertInvalidToken(Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ApiErrorCode.INVALID_TOKEN));
    }
}
