package com.tyb.myblog.v2.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorCodeTest {

    @Test
    void mapsAuthenticationFailuresToUnauthorizedAndAuthorizationFailureToForbidden() {
        assertThat(ApiErrorCode.BAD_CREDENTIALS.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ApiErrorCode.AUTHENTICATION_REQUIRED.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ApiErrorCode.INVALID_TOKEN.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ApiErrorCode.FORBIDDEN.status()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void exposesStableCodeAndChineseDefaultMessage() {
        assertThat(ApiErrorCode.VALIDATION_ERROR.code()).isEqualTo("90001");
        assertThat(ApiErrorCode.VALIDATION_ERROR.defaultMessage()).isEqualTo("参数校验失败");
        assertThat(ApiErrorCode.INTERNAL_ERROR.code()).isEqualTo("99999");
        assertThat(ApiErrorCode.INTERNAL_ERROR.defaultMessage()).isEqualTo("系统内部错误");
    }

    @Test
    void doesNotKeepAmbiguousUnauthorizedErrorCode() {
        assertThat(Arrays.stream(ApiErrorCode.values()).map(Enum::name))
                .doesNotContain("UNAUTHORIZED");
    }
}
