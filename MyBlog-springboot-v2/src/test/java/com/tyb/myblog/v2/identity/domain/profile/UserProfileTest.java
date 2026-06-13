package com.tyb.myblog.v2.identity.domain.profile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 用户资料领域规则测试。
 */
class UserProfileTest {

    @Test
    void shouldNormalizeProfileValues() {
        UserProfile profile = profile(
                1001L,
                " TYB ",
                " ",
                " 中文简介 ",
                null,
                null,
                " Tokyo ",
                " https://example.com ",
                " tyb@example.com ",
                " https://github.com/tyb ");

        assertThat(profile.nickname()).isEqualTo("TYB");
        assertThat(profile.avatarUrl()).isNull();
        assertThat(profile.bioZh()).isEqualTo("中文简介");
        assertThat(profile.location()).isEqualTo("Tokyo");
        assertThat(profile.website()).isEqualTo("https://example.com");
        assertThat(profile.emailPublic()).isEqualTo("tyb@example.com");
        assertThat(profile.githubUrl()).isEqualTo("https://github.com/tyb");
    }

    @Test
    void shouldRejectNonPositiveUserId() {
        assertThatThrownBy(() -> profile(
                0L, "TYB", null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户 ID 必须为正数");
    }

    @Test
    void shouldRejectBlankNickname() {
        assertThatThrownBy(() -> profile(
                1001L, " ", null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("昵称不能为空");
    }

    @Test
    void shouldRejectTooLongNickname() {
        assertThatThrownBy(() -> profile(
                1001L, "a".repeat(65), null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("昵称不能超过64个字符");
    }

    @Test
    void shouldRejectTooLongBio() {
        assertThatThrownBy(() -> profile(
                1001L, "TYB", null, "中".repeat(5001), null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("中文简介不能超过5000个字符");
    }

    @Test
    void shouldRejectInvalidPublicEmail() {
        assertThatThrownBy(() -> profile(
                1001L, "TYB", null, null, null, null, null, null, "invalid-email", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("公开邮箱格式错误");
    }

    @Test
    void shouldRejectNonHttpUrl() {
        assertThatThrownBy(() -> profile(
                1001L, "TYB", null, null, null, null, null, "ftp://example.com", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("个人主页仅支持 HTTP 或 HTTPS");
    }

    @Test
    void shouldRejectHttpUrlWithoutHost() {
        assertThatThrownBy(() -> profile(
                1001L, "TYB", null, null, null, null, null, "https:///profile", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("个人主页仅支持 HTTP 或 HTTPS");
    }

    private UserProfile profile(
            long userId,
            String nickname,
            String avatarUrl,
            String bioZh,
            String bioJa,
            String bioEn,
            String location,
            String website,
            String emailPublic,
            String githubUrl) {
        return UserProfile.create(
                userId,
                nickname,
                avatarUrl,
                bioZh,
                bioJa,
                bioEn,
                location,
                website,
                emailPublic,
                githubUrl,
                null,
                null,
                null,
                null,
                null);
    }
}
