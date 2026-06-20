package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.identity.application.auth.ChangePasswordApplicationService;
import com.tyb.myblog.v2.identity.application.auth.ChangePasswordCommand;
import com.tyb.myblog.v2.identity.application.profile.CurrentUserProfileQueryService;
import com.tyb.myblog.v2.identity.application.profile.CurrentUserProfileResult;
import com.tyb.myblog.v2.identity.application.profile.CurrentUserProfileUpdateService;
import com.tyb.myblog.v2.identity.application.profile.UserProfileResult;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 当前用户资料 HTTP 接口测试。
 */
@WebMvcTest(CurrentUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CurrentUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserProfileQueryService queryService;

    @MockitoBean
    private CurrentUserProfileUpdateService updateService;

    @MockitoBean
    private ChangePasswordApplicationService changePasswordService;

    private AuthenticatedPrincipal principal;
    private UserProfileResult profile;

    @BeforeEach
    void setUp() {
        principal = new AuthenticatedPrincipal(
                "1001",
                "admin",
                List.of("ADMIN"));
        profile = profile("TYB", "https://example.com/twitter");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCurrentAccountAndCompleteProfileWithoutSecurityFields()
            throws Exception {
        when(queryService.query("1001"))
                .thenReturn(new CurrentUserProfileResult(
                        9007199254740993L,
                        "admin",
                        AccountType.ADMIN,
                        profile));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.id")
                        .value("9007199254740993"))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.type").value("ADMIN"))
                .andExpect(jsonPath("$.data.profile.nickname").value("TYB"))
                .andExpect(jsonPath("$.data.profile.avatarUrl")
                        .value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.data.profile.bioZh").value("中文简介"))
                .andExpect(jsonPath("$.data.profile.bioJa").value("日本語"))
                .andExpect(jsonPath("$.data.profile.bioEn").value("English"))
                .andExpect(jsonPath("$.data.profile.location").value("Tokyo"))
                .andExpect(jsonPath("$.data.profile.website")
                        .value("https://example.com"))
                .andExpect(jsonPath("$.data.profile.emailPublic")
                        .value("public@example.com"))
                .andExpect(jsonPath("$.data.profile.githubUrl")
                        .value("https://github.com/example"))
                .andExpect(jsonPath("$.data.profile.twitterUrl")
                        .value("https://example.com/twitter"))
                .andExpect(jsonPath("$.data.profile.linkedinUrl")
                        .value("https://linkedin.com/in/example"))
                .andExpect(jsonPath("$.data.profile.zhihuUrl")
                        .value("https://www.zhihu.com/people/example"))
                .andExpect(jsonPath("$.data.profile.qiitaUrl")
                        .value("https://qiita.com/example"))
                .andExpect(jsonPath("$.data.profile.juejinUrl")
                        .value("https://juejin.cn/user/example"))
                .andExpect(jsonPath("$.data.profile.userId").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.tokenVersion").doesNotExist())
                .andExpect(jsonPath("$.data.loginFailCount").doesNotExist())
                .andExpect(jsonPath("$.data.lockedUntil").doesNotExist());
    }

    @Test
    void keepsUnsubmittedPatchFieldsAbsent() throws Exception {
        when(updateService.update(eq(principal), any()))
                .thenReturn(profile("New Name", "https://example.com/twitter"));

        mockMvc.perform(patch("/api/auth/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"New Name"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("New Name"));

        verify(updateService).update(eq(principal),
                org.mockito.ArgumentMatchers.argThat(command ->
                        command.nickname().present()
                                && "New Name".equals(command.nickname().value())
                                && !command.avatarUrl().present()
                                && !command.twitterUrl().present()
                                && !command.website().present()));
    }

    @Test
    void preservesExplicitNullAsPresentPatchValue() throws Exception {
        when(updateService.update(eq(principal), any()))
                .thenReturn(profile("TYB", null));

        mockMvc.perform(patch("/api/auth/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"twitterUrl":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.twitterUrl").isEmpty());

        verify(updateService).update(eq(principal),
                org.mockito.ArgumentMatchers.argThat(command ->
                        command.twitterUrl().present()
                                && command.twitterUrl().value() == null
                                && !command.nickname().present()));
    }

    @Test
    void rejectsEmptyPatchObject() throws Exception {
        when(updateService.update(eq(principal), any()))
                .thenThrow(new ApiException(
                        ApiErrorCode.VALIDATION_ERROR,
                        "至少提交一个资料字段"));

        assertValidationFailure("{}");
    }

    @Test
    void rejectsUnknownPatchField() throws Exception {
        assertValidationFailure("""
                {"unknownField":"value"}
                """);
        verifyNoInteractions(updateService);
    }

    @Test
    void rejectsBlankNickname() throws Exception {
        when(updateService.update(eq(principal), any()))
                .thenThrow(new ApiException(
                        ApiErrorCode.VALIDATION_ERROR,
                        "昵称不能为空"));

        assertValidationFailure("""
                {"nickname":" "}
                """);
    }

    @Test
    void mapsApplicationForbiddenError() throws Exception {
        when(updateService.update(eq(principal), any()))
                .thenThrow(new ApiException(ApiErrorCode.FORBIDDEN));

        mockMvc.perform(patch("/api/auth/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"New Name"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));
    }

    @Test
    void changesCurrentAdminPassword() throws Exception {
        mockMvc.perform(put("/api/auth/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"old-password",
                                  "newPassword":"new-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(changePasswordService).change(
                principal,
                new ChangePasswordCommand(
                        "old-password",
                        "new-password"));
    }

    @Test
    void rejectsInvalidChangePasswordRequest() throws Exception {
        assertPasswordValidationFailure("""
                {"currentPassword":"","newPassword":"new-password"}
                """);
        assertPasswordValidationFailure("""
                {"currentPassword":"old-password","newPassword":"short"}
                """);
        assertPasswordValidationFailure(
                "{\"currentPassword\":\"old-password\",\"newPassword\":\""
                        + "x".repeat(129)
                        + "\"}");
        assertPasswordValidationFailure("""
                {"currentPassword":"old-password","newPassword":
                """);

        verifyNoInteractions(changePasswordService);
    }

    @Test
    void mapsWrongCurrentPassword() throws Exception {
        org.mockito.Mockito.doThrow(
                        new ApiException(ApiErrorCode.BAD_CREDENTIALS))
                .when(changePasswordService)
                .change(eq(principal), any());

        mockMvc.perform(put("/api/auth/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"wrong-password",
                                  "newPassword":"new-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10001"));
    }

    @Test
    void mapsChangePasswordForbiddenError() throws Exception {
        org.mockito.Mockito.doThrow(
                        new ApiException(ApiErrorCode.FORBIDDEN))
                .when(changePasswordService)
                .change(eq(principal), any());

        mockMvc.perform(put("/api/auth/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"old-password",
                                  "newPassword":"new-password"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));
    }

    private void assertValidationFailure(String content) throws Exception {
        mockMvc.perform(patch("/api/auth/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
    }

    private void assertPasswordValidationFailure(String content)
            throws Exception {
        mockMvc.perform(put("/api/auth/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
    }

    private UserProfileResult profile(String nickname, String twitterUrl) {
        return new UserProfileResult(
                nickname,
                "https://example.com/avatar.png",
                "中文简介",
                "日本語",
                "English",
                "Tokyo",
                "https://example.com",
                "public@example.com",
                "https://github.com/example",
                twitterUrl,
                "https://linkedin.com/in/example",
                "https://www.zhihu.com/people/example",
                "https://qiita.com/example",
                "https://juejin.cn/user/example");
    }
}
