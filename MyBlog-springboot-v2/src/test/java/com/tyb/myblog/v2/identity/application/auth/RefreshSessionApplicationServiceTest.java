package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * refresh 会话入口服务单元测试。
 */
class RefreshSessionApplicationServiceTest {

    private final RefreshSessionTransactionService transactionService =
            mock(RefreshSessionTransactionService.class);
    private final RefreshSessionApplicationService service =
            new RefreshSessionApplicationService(transactionService);

    @Test
    void shouldRejectBlankTokenWithoutOpeningTransaction() {
        assertInvalidToken(() -> service.refresh(" "));
        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldReturnTransactionResult() {
        LoginTokenResult expected =
                new LoginTokenResult("access", "refresh", 900, 604800);
        when(transactionService.refresh("raw-token"))
                .thenReturn(Optional.of(expected));

        assertThat(service.refresh("raw-token")).isEqualTo(expected);
    }

    @Test
    void shouldMapMissingSessionToInvalidTokenWithoutLeakingRawToken() {
        String rawToken = "sensitive-refresh-token";
        when(transactionService.refresh(rawToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(rawToken))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code())
                            .isEqualTo(ApiErrorCode.INVALID_TOKEN);
                    assertThat(exception.getMessage()).doesNotContain(rawToken);
                });
    }

    private void assertInvalidToken(Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ApiErrorCode.INVALID_TOKEN));
    }
}
