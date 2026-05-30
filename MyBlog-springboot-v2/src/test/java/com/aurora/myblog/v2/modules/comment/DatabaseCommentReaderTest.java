package com.aurora.myblog.v2.modules.comment;

import com.aurora.myblog.v2.modules.comment.domain.CommentPageQuery;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import com.aurora.myblog.v2.modules.comment.infrastructure.DatabaseCommentReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseCommentReader.class)
class DatabaseCommentReaderTest {

    @Autowired
    private DatabaseCommentReader reader;

    @Test
    void listsApprovedArticleCommentsWithReplies() {
        var page = reader.listComments(CommentType.ARTICLE, 1, new CommentPageQuery(1, 10));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.records()).extracting("id").containsExactly(1);
        assertThat(page.records().get(0).content()).isEqualTo("第一条文章评论");
        assertThat(page.records().get(0).author().nickname()).isEqualTo("普通用户");
        assertThat(page.records().get(0).replies()).extracting("id").containsExactly(2);
        assertThat(page.records().get(0).replies().get(0).replyUser().nickname()).isEqualTo("普通用户");
    }

    @Test
    void publicCommentReaderHidesPendingAndDeletedComments() {
        var page = reader.listComments(CommentType.ARTICLE, 1, new CommentPageQuery(1, 20));

        assertThat(page.records()).extracting("id").contains(1);
        assertThat(page.records()).extracting("id").doesNotContain(3, 4);
    }

    @Test
    void listsMessageCommentsWithoutTopicId() {
        var page = reader.listComments(CommentType.MESSAGE, null, new CommentPageQuery(1, 10));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.records()).extracting("id").containsExactly(5);
        assertThat(page.records().get(0).replies()).extracting("id").containsExactly(6);
    }

    @Test
    void listsRepliesByRootCommentId() {
        var replies = reader.listRepliesByCommentId(1);

        assertThat(replies).extracting("id").containsExactly(2);
        assertThat(replies.get(0).content()).isEqualTo("管理员回复普通用户");
    }

    @Test
    void listsLatestApprovedRootComments() {
        var comments = reader.listTopComments(6);

        assertThat(comments).extracting("id").containsExactly(5, 1);
    }
}
