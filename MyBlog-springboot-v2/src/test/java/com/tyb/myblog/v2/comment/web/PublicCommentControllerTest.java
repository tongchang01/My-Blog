package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.CommentCreateCommand;
import com.tyb.myblog.v2.comment.application.CommentCreateResult;
import com.tyb.myblog.v2.comment.application.CommentCreateService;
import com.tyb.myblog.v2.comment.application.CommentPageResult;
import com.tyb.myblog.v2.comment.application.CommentQueryService;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.common.web.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        PublicArticleCommentController.class,
        PublicGuestbookCommentController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentQueryService queryService;

    @MockitoBean
    private CommentCreateService createService;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @Test
    void returnsPublicCommentsWithoutSensitiveFields() throws Exception {
        when(queryService.articleComments(100L, 1, 20))
                .thenReturn(new CommentPageResult(
                        List.of(item()),
                        1,
                        1,
                        20));

        mockMvc.perform(get("/api/public/articles/100/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].contentHtml")
                        .value("<p>hello</p>"))
                .andExpect(jsonPath("$.data.records[0].contentMd")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.records[0].authorEmail")
                        .doesNotExist());
    }

    @Test
    void createsArticleAndGuestbookComments() throws Exception {
        when(createService.createArticleComment(any(CommentCreateCommand.class)))
                .thenReturn(new CommentCreateResult(200L, CommentAuditStatus.PASS));
        when(createService.createGuestbookComment(any(CommentCreateCommand.class)))
                .thenReturn(new CommentCreateResult(201L, CommentAuditStatus.PASS));
        when(clientIpResolver.resolve(any()))
                .thenReturn("127.0.0.1");
        String body = """
                {"nickname":"TYB","email":"tyb@example.com","contentMd":"hello"}
                """;

        mockMvc.perform(post("/api/public/articles/100/comments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(200));
        mockMvc.perform(post("/api/public/guestbook/comments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(201));

        verify(createService).createArticleComment(any(CommentCreateCommand.class));
        verify(createService).createGuestbookComment(any(CommentCreateCommand.class));
    }

    private static CommentPageResult.Item item() {
        return new CommentPageResult.Item(
                1L,
                null,
                null,
                null,
                "TYB",
                "https://example.com",
                "<p>hello</p>",
                LocalDateTime.of(2026, 6, 17, 19, 40),
                List.of());
    }
}
