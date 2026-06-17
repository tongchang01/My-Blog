package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.CommentPage;
import com.tyb.myblog.v2.comment.domain.CommentPageItem;
import com.tyb.myblog.v2.comment.domain.CommentQueryRepository;
import com.tyb.myblog.v2.comment.domain.CommentTarget;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicy;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicyService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentQueryServiceTest {

    private final CommentQueryRepository repository =
            mock(CommentQueryRepository.class);
    private final ArticleCommentPolicyService policyService =
            mock(ArticleCommentPolicyService.class);
    private final CommentQueryService service =
            new CommentQueryService(repository, policyService);

    @Test
    void queriesArticleCommentsAfterPolicyCheck() {
        when(policyService.requirePublicCommentable(100L))
                .thenReturn(new ArticleCommentPolicy(100L, 1));
        when(repository.page(CommentTarget.article(100L), 1, 20))
                .thenReturn(new CommentPage(List.of(item(1L, null)), 1, 1, 20));

        CommentPageResult result = service.articleComments(100L, 1, 20);

        assertThat(result.records()).hasSize(1);
        verify(policyService).requirePublicCommentable(100L);
    }

    @Test
    void queriesGuestbookWithoutArticlePolicy() {
        when(repository.page(CommentTarget.guestbook(), 1, 20))
                .thenReturn(new CommentPage(List.of(item(2L, null)), 1, 1, 20));

        CommentPageResult result = service.guestbookComments(1, 20);

        assertThat(result.records()).extracting(CommentPageResult.Item::id)
                .containsExactly(2L);
    }

    private static CommentPageItem item(long id, Long parentId) {
        return new CommentPageItem(
                id,
                parentId,
                null,
                null,
                "TYB",
                "https://example.com",
                "<p>hello</p>",
                LocalDateTime.of(2026, 6, 17, 19, 40),
                List.of());
    }
}
