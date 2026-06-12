package com.tyb.myblog.v2.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorCodeTest {

    @Test
    void mapsAuthenticationFailuresToUnauthorizedAndAuthorizationFailureToForbidden() {
        assertThat(ApiErrorCode.BAD_CREDENTIALS.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ApiErrorCode.INVALID_TOKEN.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ApiErrorCode.FORBIDDEN.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ApiErrorCode.INVALID_TOKEN.code()).isEqualTo("10002");
        assertThat(ApiErrorCode.FORBIDDEN.code()).isEqualTo("10003");
    }

    @Test
    void exposesStableCodeAndChineseDefaultMessage() {
        assertThat(ApiErrorCode.VALIDATION_ERROR.code()).isEqualTo("90001");
        assertThat(ApiErrorCode.VALIDATION_ERROR.defaultMessage()).isEqualTo("参数校验失败");
        assertThat(ApiErrorCode.INTERNAL_ERROR.code()).isEqualTo("99999");
        assertThat(ApiErrorCode.INTERNAL_ERROR.defaultMessage()).isEqualTo("系统内部错误");
    }

    @Test
    void shouldExposeRateLimitError() {
        assertThat(ApiErrorCode.RATE_LIMITED.code()).isEqualTo("90002");
        assertThat(ApiErrorCode.RATE_LIMITED.status())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(ApiErrorCode.RATE_LIMITED.defaultMessage())
                .isEqualTo("请求过于频繁");
    }

    @Test
    void doesNotKeepAmbiguousUnauthorizedErrorCode() {
        assertThat(Arrays.stream(ApiErrorCode.values()).map(Enum::name))
                .doesNotContain("UNAUTHORIZED", "AUTHENTICATION_REQUIRED");
    }
}
