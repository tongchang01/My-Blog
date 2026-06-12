package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.common.web.ClientIpResolver;
import com.tyb.myblog.v2.identity.application.auth.AuthApplicationService;
import com.tyb.myblog.v2.identity.application.auth.LoginCommand;
import com.tyb.myblog.v2.identity.application.auth.LoginTokenResult;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 后台登录 Controller 测试。
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthApplicationService authApplicationService;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @Test
    void returnsLoginTokensAndPassesTrustedClientIp() throws Exception {
        when(clientIpResolver.resolve(any(HttpServletRequest.class)))
                .thenReturn("203.0.113.10");
        when(authApplicationService.login(new LoginCommand(
                " Admin ",
                "secret",
                "203.0.113.10")))
                .thenReturn(new LoginTokenResult(
                        "access-value",
                        "refresh-value",
                        900,
                        604800));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":" Admin ","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.accessToken").value("access-value"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-value"))
                .andExpect(jsonPath("$.data.accessExpiresIn").value(900))
                .andExpect(jsonPath("$.data.refreshExpiresIn").value(604800));
    }

    @Test
    void rejectsBlankUsername() throws Exception {
        assertValidationFailure("""
                {"username":"","password":"secret"}
                """);
    }

    @Test
    void rejectsUsernameLongerThan64Characters() throws Exception {
        assertValidationFailure("""
                {"username":"%s","password":"secret"}
                """.formatted("u".repeat(65)));
    }

    @Test
    void rejectsBlankPassword() throws Exception {
        assertValidationFailure("""
                {"username":"admin","password":""}
                """);
    }

    @Test
    void rejectsPasswordLongerThan128Characters() throws Exception {
        assertValidationFailure("""
                {"username":"admin","password":"%s"}
                """.formatted("p".repeat(129)));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        assertValidationFailure("""
                {"username":"admin","password":
                """);
    }

    private void assertValidationFailure(String body) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"))
                .andExpect(jsonPath("$.data").isEmpty());

        verifyNoInteractions(authApplicationService);
    }
}
