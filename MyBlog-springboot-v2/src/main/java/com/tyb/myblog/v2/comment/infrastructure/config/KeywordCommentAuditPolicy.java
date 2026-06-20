package com.tyb.myblog.v2.comment.infrastructure.config;

import com.tyb.myblog.v2.comment.application.CommentAuditPolicy;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 基于配置关键词的乐观评论审核策略。
 */
@Component
@RequiredArgsConstructor
public class KeywordCommentAuditPolicy implements CommentAuditPolicy {

    private final CommentAuditProperties properties;

    @Override
    public CommentAuditStatus audit(String contentMarkdown) {
        if (contentMarkdown == null) {
            return CommentAuditStatus.PASS;
        }
        String normalized = contentMarkdown.toLowerCase(Locale.ROOT);
        return properties.blockedKeywords().stream()
                .anyMatch(normalized::contains)
                ? CommentAuditStatus.PENDING
                : CommentAuditStatus.PASS;
    }
}
