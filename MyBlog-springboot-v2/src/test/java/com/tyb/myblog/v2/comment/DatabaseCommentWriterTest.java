package com.tyb.myblog.v2.comment;

import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.comment.domain.CommentCreateCommand;
import com.tyb.myblog.v2.comment.domain.CommentType;
import com.tyb.myblog.v2.comment.infrastructure.DatabaseCommentWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseCommentWriter.class)
class DatabaseCommentWriterTest {

    @Autowired
    private DatabaseCommentWriter writer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesArticleRootCommentAsPendingReview() {
        int id = writer.save(new CommentCreateCommand(
                2,
                CommentType.ARTICLE,
                1,
                null,
                null,
                "新的文章评论",
                "203.0.113.77",
                "JUnit Browser"));

        Integer isReview = jdbcTemplate.queryForObject("select is_review from t_comment where id = ?", Integer.class, id);
        String content = jdbcTemplate.queryForObject("select comment_content from t_comment where id = ?", String.class, id);
        String createIp = jdbcTemplate.queryForObject("select create_ip from t_comment where id = ?", String.class, id);
        String userAgent = jdbcTemplate.queryForObject("select user_agent from t_comment where id = ?", String.class, id);

        assertThat(isReview).isZero();
        assertThat(content).isEqualTo("新的文章评论");
        assertThat(createIp).isEqualTo("203.0.113.77");
        assertThat(userAgent).isEqualTo("JUnit Browser");
    }

    @Test
    void rejectsArticleCommentWithoutTopicId() {
        assertThatThrownBy(() -> writer.save(new CommentCreateCommand(
                2,
                CommentType.ARTICLE,
                null,
                null,
                null,
                "缺少文章",
                "203.0.113.77",
                "JUnit Browser")))
                .isInstanceOf(ApiException.class)
                .hasMessage("文章评论必须指定文章");
    }

    @Test
    void rejectsReplyToNonRootComment() {
        assertThatThrownBy(() -> writer.save(new CommentCreateCommand(
                2,
                CommentType.ARTICLE,
                1,
                2,
                1,
                "不能回复回复",
                "203.0.113.77",
                "JUnit Browser")))
                .isInstanceOf(ApiException.class)
                .hasMessage("只能回复根评论");
    }
}
