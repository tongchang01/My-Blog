package com.tyb.myblog.v2.comment.domain;

public record CommentTarget(
        CommentTargetType targetType,
        long targetId) {

    public CommentTarget {
        if (targetType == null) {
            throw new IllegalArgumentException("评论目标类型不能为空");
        }
        if (targetType == CommentTargetType.ARTICLE && targetId <= 0) {
            throw new IllegalArgumentException("文章评论目标 ID 必须为正数");
        }
        if (targetType == CommentTargetType.GUESTBOOK && targetId != 0) {
            throw new IllegalArgumentException("留言板评论目标 ID 必须为 0");
        }
    }

    public static CommentTarget article(long articleId) {
        return new CommentTarget(CommentTargetType.ARTICLE, articleId);
    }

    public static CommentTarget guestbook() {
        return new CommentTarget(CommentTargetType.GUESTBOOK, 0);
    }

    public static CommentTarget of(
            CommentTargetType targetType,
            long targetId) {
        return new CommentTarget(targetType, targetId);
    }
}
