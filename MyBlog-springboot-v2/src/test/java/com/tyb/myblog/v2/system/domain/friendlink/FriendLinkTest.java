package com.tyb.myblog.v2.system.domain.friendlink;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FriendLinkTest {

    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(2026, 6, 14, 12, 0);

    @Test
    void parsesStableStatusValues() {
        assertThat(FriendLinkStatus.fromDatabaseValue(1))
                .isEqualTo(FriendLinkStatus.VISIBLE);
        assertThat(FriendLinkStatus.fromDatabaseValue(2))
                .isEqualTo(FriendLinkStatus.HIDDEN);
        assertThatThrownBy(
                () -> FriendLinkStatus.fromDatabaseValue(3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesBusinessFields() {
        FriendLink link = friendLink(
                " Example ",
                " https://example.com/path ",
                " https://example.com/logo.png ",
                " 介绍 ",
                20);

        assertThat(link.name()).isEqualTo("Example");
        assertThat(link.url()).isEqualTo("https://example.com/path");
        assertThat(link.avatarUrl())
                .isEqualTo("https://example.com/logo.png");
        assertThat(link.description()).isEqualTo("介绍");
    }

    @Test
    void normalizesBlankOptionalFieldsToNull() {
        FriendLink link = friendLink(
                "Example",
                "https://example.com",
                " ",
                "\t",
                0);

        assertThat(link.avatarUrl()).isNull();
        assertThat(link.description()).isNull();
    }

    @Test
    void rejectsInvalidIdentityAndTextFields() {
        assertThatThrownBy(() -> FriendLink.reconstitute(
                0,
                "Example",
                "https://example.com",
                null,
                null,
                0,
                FriendLinkStatus.VISIBLE,
                CREATED_AT,
                1001L,
                CREATED_AT,
                1001L))
                .isInstanceOf(IllegalArgumentException.class);
        assertInvalidName(" ");
        assertInvalidName("x".repeat(65));
        assertThatThrownBy(() -> friendLink(
                "Example",
                "https://example.com",
                null,
                "x".repeat(256),
                0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsupportedUrls() {
        assertInvalidUrl("/relative");
        assertInvalidUrl("ftp://example.com");
        assertInvalidUrl("https:///missing-host");
        assertInvalidUrl("https://user@example.com");
        assertInvalidUrl("https://example.com/" + "x".repeat(240));
        assertThatThrownBy(() -> friendLink(
                "Example",
                "https://example.com",
                "ftp://example.com/logo.png",
                null,
                0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidSortOrder() {
        assertInvalidSortOrder(-1);
        assertInvalidSortOrder(1_000_001);
    }

    @Test
    void createsNewFriendLinkWithoutIdentityAndAllowsDuplicateUrls() {
        NewFriendLink first = NewFriendLink.create(
                "First",
                "https://example.com",
                null,
                null,
                0,
                FriendLinkStatus.VISIBLE,
                1001L);
        NewFriendLink second = NewFriendLink.create(
                "Second",
                "https://example.com",
                null,
                null,
                1,
                FriendLinkStatus.HIDDEN,
                1001L);

        assertThat(first.url()).isEqualTo(second.url());
        assertThatThrownBy(() -> NewFriendLink.create(
                "Example",
                "https://example.com",
                null,
                null,
                0,
                FriendLinkStatus.VISIBLE,
                0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private FriendLink friendLink(
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder) {
        return FriendLink.reconstitute(
                10L,
                name,
                url,
                avatarUrl,
                description,
                sortOrder,
                FriendLinkStatus.VISIBLE,
                CREATED_AT,
                1001L,
                CREATED_AT.plusMinutes(30),
                1001L);
    }

    private void assertInvalidName(String name) {
        assertThatThrownBy(() -> friendLink(
                name,
                "https://example.com",
                null,
                null,
                0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void assertInvalidUrl(String url) {
        assertThatThrownBy(() -> friendLink(
                "Example",
                url,
                null,
                null,
                0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void assertInvalidSortOrder(int sortOrder) {
        assertThatThrownBy(() -> friendLink(
                "Example",
                "https://example.com",
                null,
                null,
                sortOrder))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
