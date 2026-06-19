package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.AdminCommentPage;
import com.tyb.myblog.v2.comment.domain.AdminCommentPageItem;
import com.tyb.myblog.v2.comment.domain.AdminCommentQueryCriteria;
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

    private static final AdminCommentPageQuery QUERY =
            new AdminCommentPageQuery(
                    CommentTargetType.ARTICLE,
                    100L,
                    CommentAuditStatus.PENDING,
                    "hello",
                    false,
                    1,
                    20);

    private static final AdminCommentQueryCriteria CRITERIA =
            new AdminCommentQueryCriteria(
                    CommentTargetType.ARTICLE,
                    100L,
                    CommentAuditStatus.PENDING,
                    "hello",
                    false,
                    1,
                    20);

    private final AdminCommentQueryRepository repository =
            mock(AdminCommentQueryRepository.class);
    private final AdminCommentQueryService service =
            new AdminCommentQueryService(repository, new CommentAuthorization());

    @Test
    void adminCanReadCommentAuditFields() {
        when(repository.page(CRITERIA))
                .thenReturn(new AdminCommentPage(List.of(item()), 1, 1, 20));

        AdminCommentPageResult.Item result = service.page(
                principal("ADMIN"), QUERY).records().get(0);

        assertThat(result.authorEmail()).isEqualTo("tyb@example.com");
        assertThat(result.authorIp()).isEqualTo("127.0.0.1");
        assertThat(result.authorUserAgent()).isEqualTo("JUnit");
        verify(repository).page(CRITERIA);
    }

    @Test
    void demoCanReadCommentWithoutAuditFields() {
        when(repository.page(CRITERIA))
                .thenReturn(new AdminCommentPage(List.of(item()), 1, 1, 20));

        AdminCommentPageResult.Item result = service.page(
                principal("DEMO"), QUERY).records().get(0);

        assertThat(result.authorEmail()).isNull();
        assertThat(result.authorIp()).isNull();
        assertThat(result.authorUserAgent()).isNull();
        assertThat(result.authorNickname()).isEqualTo("TYB");
        assertThat(result.contentMd()).isEqualTo("hello");
        verify(repository).page(CRITERIA);
    }

    private static AuthenticatedPrincipal principal(String role) {
        return new AuthenticatedPrincipal(
                "1001",
                role.toLowerCase(),
                List.of(role));
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
