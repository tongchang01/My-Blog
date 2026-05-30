package com.aurora.myblog.v2.modules.comment;

import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDeletionCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerationCommand;
import com.aurora.myblog.v2.modules.comment.infrastructure.DatabaseAdminCommentModerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DatabaseAdminCommentModeratorTest {

    @Autowired
    private DatabaseAdminCommentModerator moderator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reviewsCommentsInBatch() {
        moderator.review(new AdminCommentModerationCommand(List.of(3), true));

        Integer reviewed = jdbcTemplate.queryForObject("select is_review from t_comment where id = 3", Integer.class);
        assertThat(reviewed).isEqualTo(1);
    }

    @Test
    void cancelsReviewInBatch() {
        moderator.review(new AdminCommentModerationCommand(List.of(1, 2), false));

        Integer first = jdbcTemplate.queryForObject("select is_review from t_comment where id = 1", Integer.class);
        Integer second = jdbcTemplate.queryForObject("select is_review from t_comment where id = 2", Integer.class);
        assertThat(first).isZero();
        assertThat(second).isZero();
    }

    @Test
    void softDeletesCommentsInBatch() {
        moderator.delete(new AdminCommentDeletionCommand(List.of(1, 2)));

        Integer first = jdbcTemplate.queryForObject("select is_delete from t_comment where id = 1", Integer.class);
        Integer second = jdbcTemplate.queryForObject("select is_delete from t_comment where id = 2", Integer.class);
        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(1);
    }

    @Test
    void rejectsEmptyIds() {
        assertThatThrownBy(() -> moderator.review(new AdminCommentModerationCommand(List.of(), true)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("评论 ID 不能为空");
    }
}
