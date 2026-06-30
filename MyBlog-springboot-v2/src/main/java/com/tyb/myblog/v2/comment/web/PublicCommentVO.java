package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.CommentPageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record PublicCommentVO(
        @Schema(format = "int64") String id,
        @Schema(format = "int64") String parentId,
        @Schema(format = "int64") String replyToCommentId,
        String replyToNickname,
        String authorNickname,
        String authorSite,
        String contentHtml,
        LocalDateTime createdAt,
        List<PublicCommentVO> replies) {

    public static PublicCommentVO from(CommentPageResult.Item item) {
        return new PublicCommentVO(
                id(item.id()),
                nullableId(item.parentId()),
                nullableId(item.replyToCommentId()),
                item.replyToNickname(),
                item.authorNickname(),
                item.authorSite(),
                item.contentHtml(),
                item.createdAt(),
                item.replies().stream()
                        .map(PublicCommentVO::from)
                        .toList());
    }

    private static String id(long value) {
        return Long.toString(value);
    }

    private static String nullableId(Long value) {
        return value == null ? null : Long.toString(value);
    }
}
