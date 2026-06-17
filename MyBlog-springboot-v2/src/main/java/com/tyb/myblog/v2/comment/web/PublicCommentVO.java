package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.CommentPageResult;

import java.time.LocalDateTime;
import java.util.List;

public record PublicCommentVO(
        long id,
        Long parentId,
        Long replyToCommentId,
        String replyToNickname,
        String authorNickname,
        String authorSite,
        String contentHtml,
        LocalDateTime createdAt,
        List<PublicCommentVO> replies) {

    public static PublicCommentVO from(CommentPageResult.Item item) {
        return new PublicCommentVO(
                item.id(),
                item.parentId(),
                item.replyToCommentId(),
                item.replyToNickname(),
                item.authorNickname(),
                item.authorSite(),
                item.contentHtml(),
                item.createdAt(),
                item.replies().stream()
                        .map(PublicCommentVO::from)
                        .toList());
    }
}
