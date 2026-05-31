package com.tyb.myblog.v2.comment.domain;

/**
 * 后台评论查询条件。
 *
 * @param type     评论类型
 * @param topicId  主题 ID
 * @param reviewed 审核状态筛选
 * @param keyword  评论内容或用户昵称关键词
 * @param deleted  是否查询已软删除评论
 * @param page     页码
 * @param size     每页大小
 */
public record AdminCommentQuery(
        CommentType type,
        Integer topicId,
        Boolean reviewed,
        String keyword,
        Boolean deleted,
        int page,
        int size
) {
    /**
     * 默认查询未删除评论。
     */
    public AdminCommentQuery(CommentType type, Integer topicId, Boolean reviewed, String keyword, int page, int size) {
        this(type, topicId, reviewed, keyword, false, page, size);
    }

    /**
     * 规范化分页和关键词。
     */
    public AdminCommentQuery {
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        deleted = deleted == null ? false : deleted;
    }

    /**
     * 计算 SQL 查询偏移量。
     */
    public int offset() {
        return (page - 1) * size;
    }
}
