package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.AdminCommentCommandService;
import com.tyb.myblog.v2.comment.application.AdminCommentPageResult;
import com.tyb.myblog.v2.comment.application.AdminCommentQueryService;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentTargetType;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCommentQueryService queryService;

    @MockitoBean
    private AdminCommentCommandService commandService;

    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new AuthenticatedPrincipal(
                "1001", "admin", List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAdminCommentPage() throws Exception {
        when(queryService.page(eq(principal), any()))
                .thenReturn(new AdminCommentPageResult(List.of(item()), 1, 1, 20));

        mockMvc.perform(get("/api/admin/comments")
                        .param("targetType", "ARTICLE")
                        .param("auditStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].authorEmail")
                        .value("tyb@example.com"))
                .andExpect(jsonPath("$.data.records[0].contentMd")
                        .value("hello"));
    }

    @Test
    void delegatesModerationCommands() throws Exception {
        mockMvc.perform(post("/api/admin/comments/10/approve"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/comments/10/hide"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/comments/10/restore"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/comments/10"))
                .andExpect(status().isOk());

        verify(commandService).approve(principal, 10L);
        verify(commandService).hide(principal, 10L);
        verify(commandService).restore(principal, 10L);
        verify(commandService).delete(principal, 10L);
    }

    private static AdminCommentPageResult.Item item() {
        return new AdminCommentPageResult.Item(
                10L,
                CommentTargetType.ARTICLE,
                100L,
                null,
                null,
                null,
                "TYB",
                "tyb@example.com",
                "https://example.com",
                "127.0.0.1",
                "JUnit",
                "hello",
                "<p>hello</p>",
                CommentAuditStatus.PENDING,
                LocalDateTime.of(2026, 6, 17, 19, 50),
                false);
    }
}
