package com.tyb.myblog.v2.common.validation;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class PublicHttpUrlTest {

    @Test
    void acceptsOnlyAbsoluteHttpUrlsWithoutCredentials() {
        assertThat(PublicHttpUrl.isValid(
                URI.create("https://example.com/path?next=user@example.com")))
                .isTrue();
        assertThat(PublicHttpUrl.isValid(
                URI.create("http://localhost:8080/media")))
                .isTrue();
        assertThat(PublicHttpUrl.isValid(
                URI.create("https://user:password@example.com")))
                .isFalse();
        assertThat(PublicHttpUrl.isValid(
                URI.create("ftp://example.com/file")))
                .isFalse();
        assertThat(PublicHttpUrl.isValid(
                URI.create("/relative")))
                .isFalse();
    }
}
