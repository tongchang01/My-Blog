package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.AdminCommentPage;
import com.tyb.myblog.v2.comment.domain.AdminCommentPageItem;
import com.tyb.myblog.v2.comment.domain.AdminCommentQueryRepository;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentTargetType;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCommentQueryServiceTest {

    private final AdminCommentQueryRepository repository =
            mock(AdminCommentQueryRepository.class);
    private final AdminCommentQueryService service =
            new AdminCommentQueryService(repository, new CommentAuthorization());

    @Test
    void adminAndDemoCanReadCommentPage() {
        AdminCommentPageQuery query = new AdminCommentPageQuery(
                CommentTargetType.ARTICLE,
                100L,
                CommentAuditStatus.PENDING,
                "hello",
                false,
                1,
                20);
        when(repository.page(query))
                .thenReturn(new AdminCommentPage(List.of(item()), 1, 1, 20));

        AdminCommentPageResult result = service.page(
                new AuthenticatedPrincipal("1002", "demo", List.of("DEMO")),
                query);

        assertThat(result.records()).hasSize(1);
        verify(repository).page(query);
    }

    private static AdminCommentPageItem item() {
        return new AdminCommentPageItem(
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
