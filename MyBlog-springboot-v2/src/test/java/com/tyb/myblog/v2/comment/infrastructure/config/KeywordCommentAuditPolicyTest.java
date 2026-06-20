package com.tyb.myblog.v2.comment.infrastructure.config;

import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordCommentAuditPolicyTest {

    @Test
    void marksNormalizedKeywordMatchesAsPending() {
        KeywordCommentAuditPolicy policy = new KeywordCommentAuditPolicy(
                new CommentAuditProperties(List.of(" spam ", "广告")));

        assertThat(policy.audit("This is SPAM"))
                .isEqualTo(CommentAuditStatus.PENDING);
        assertThat(policy.audit("包含广告内容"))
                .isEqualTo(CommentAuditStatus.PENDING);
        assertThat(policy.audit("普通评论"))
                .isEqualTo(CommentAuditStatus.PASS);
    }

    @Test
    void ignoresBlankAndDuplicateKeywords() {
        CommentAuditProperties properties = new CommentAuditProperties(
                java.util.Arrays.asList(null, " ", "Spam", " spam "));
        KeywordCommentAuditPolicy policy = new KeywordCommentAuditPolicy(properties);

        assertThat(properties.blockedKeywords()).containsExactly("spam");
        assertThat(policy.audit("ordinary comment"))
                .isEqualTo(CommentAuditStatus.PASS);
    }
}
