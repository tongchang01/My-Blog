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

    private static final long COMMENT_ID = 9007199254740993L;
    private static final long REPLY_ID = 9007199254740995L;
    private static final long CREATED_ARTICLE_COMMENT_ID = 9007199254740997L;
    private static final long CREATED_GUESTBOOK_COMMENT_ID = 9007199254740999L;

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
                .andExpect(jsonPath("$.data.records[0].id")
                        .value(Long.toString(COMMENT_ID)))
                .andExpect(jsonPath("$.data.records[0].parentId")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.records[0].replyToCommentId")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.records[0].replies[0].id")
                        .value(Long.toString(REPLY_ID)))
                .andExpect(jsonPath("$.data.records[0].replies[0].parentId")
                        .value(Long.toString(COMMENT_ID)))
                .andExpect(jsonPath("$.data.records[0].replies[0].replyToCommentId")
                        .value(Long.toString(COMMENT_ID)))
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
                .thenReturn(new CommentCreateResult(
                        CREATED_ARTICLE_COMMENT_ID,
                        CommentAuditStatus.PASS));
        when(createService.createGuestbookComment(any(CommentCreateCommand.class)))
                .thenReturn(new CommentCreateResult(
                        CREATED_GUESTBOOK_COMMENT_ID,
                        CommentAuditStatus.PASS));
        when(clientIpResolver.resolve(any()))
                .thenReturn("127.0.0.1");
        String body = """
                {"nickname":"TYB","email":"tyb@example.com","contentMd":"hello"}
                """;

        mockMvc.perform(post("/api/public/articles/100/comments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(Long.toString(CREATED_ARTICLE_COMMENT_ID)));
        mockMvc.perform(post("/api/public/guestbook/comments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(Long.toString(CREATED_GUESTBOOK_COMMENT_ID)));

        verify(createService).createArticleComment(any(CommentCreateCommand.class));
        verify(createService).createGuestbookComment(any(CommentCreateCommand.class));
    }

    @Test
    void rejectsInvalidPublicCommentRequest() throws Exception {
        mockMvc.perform(post("/api/public/articles/100/comments")
                        .contentType("application/json")
                        .content("""
                                {"nickname":"","email":"tyb@example.com","contentMd":"hello"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
    }

    private static CommentPageResult.Item item() {
        return new CommentPageResult.Item(
                COMMENT_ID,
                null,
                null,
                null,
                "TYB",
                "https://example.com",
                "<p>hello</p>",
                LocalDateTime.of(2026, 6, 17, 19, 40),
                List.of(new CommentPageResult.Item(
                        REPLY_ID,
                        COMMENT_ID,
                        COMMENT_ID,
                        "TYB",
                        "Reader",
                        null,
                        "<p>reply</p>",
                        LocalDateTime.of(2026, 6, 17, 19, 41),
                        List.of())));
    }
}
