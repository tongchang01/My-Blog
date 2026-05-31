package com.tyb.myblog.v2.content;

import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.content.infrastructure.SignedArticleAccessTokenService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SignedArticleAccessTokenServiceTest {

    private final SignedArticleAccessTokenService service = new SignedArticleAccessTokenService(
            new SecurityJwtProperties("myblog-v2-test", "test-secret-test-secret-test-secret-123456", Duration.ofMinutes(15)));

    @Test
    void issuesAndVerifiesArticleAccessToken() {
        var token = service.issue(3);

        assertThat(token.value()).isNotBlank();
        assertThat(token.expiresAt()).isNotNull();
        assertThat(service.verify(3, token.value())).isTrue();
    }

    @Test
    void rejectsTokenForDifferentArticle() {
        var token = service.issue(3);

        assertThat(service.verify(4, token.value())).isFalse();
    }

    @Test
    void rejectsTamperedToken() {
        var token = service.issue(3);

        assertThat(service.verify(3, token.value() + "x")).isFalse();
    }
}
