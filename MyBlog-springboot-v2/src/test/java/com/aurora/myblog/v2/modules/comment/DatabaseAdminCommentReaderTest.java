package com.aurora.myblog.v2.modules.comment;

import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentItem;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentQuery;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import com.aurora.myblog.v2.modules.comment.infrastructure.DatabaseAdminCommentReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase
class DatabaseAdminCommentReaderTest {

    @Autowired
    private DatabaseAdminCommentReader reader;

    @Test
    void listsNonDeletedCommentsForAdmin() {
        PageResponse<AdminCommentItem> page = reader.list(new AdminCommentQuery(null, null, null, null, 1, 10));

        assertThat(page.total()).isEqualTo(5);
        assertThat(page.records()).extracting(AdminCommentItem::id).containsExactly(6, 5, 3, 2, 1);
        assertThat(page.records().get(2).reviewed()).isFalse();
        assertThat(page.records().get(2).deleted()).isFalse();
    }

    @Test
    void filtersByTypeTopicAndReviewStatus() {
        PageResponse<AdminCommentItem> page = reader.list(new AdminCommentQuery(CommentType.ARTICLE, 1, false, null, 1, 10));

        assertThat(page.total()).isEqualTo(1);
        AdminCommentItem item = page.records().get(0);
        assertThat(item.id()).isEqualTo(3);
        assertThat(item.type()).isEqualTo(CommentType.ARTICLE);
        assertThat(item.topicId()).isEqualTo(1);
        assertThat(item.topicTitle()).isEqualTo("后端V2第一篇");
        assertThat(item.reviewed()).isFalse();
    }

    @Test
    void searchesByCommentContentAndAuthorNickname() {
        PageResponse<AdminCommentItem> byContent = reader.list(new AdminCommentQuery(null, null, null, "待审核", 1, 10));
        PageResponse<AdminCommentItem> byAuthor = reader.list(new AdminCommentQuery(null, null, null, "普通用户", 1, 10));

        assertThat(byContent.records()).extracting(AdminCommentItem::id).containsExactly(3);
        assertThat(byAuthor.total()).isGreaterThanOrEqualTo(1);
    }
}
