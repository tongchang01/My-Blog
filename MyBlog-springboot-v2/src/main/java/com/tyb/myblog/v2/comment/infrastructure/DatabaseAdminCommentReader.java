package com.tyb.myblog.v2.comment.infrastructure;

import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.comment.domain.AdminCommentDetail;
import com.tyb.myblog.v2.comment.domain.AdminCommentItem;
import com.tyb.myblog.v2.comment.domain.AdminCommentQuery;
import com.tyb.myblog.v2.comment.domain.AdminCommentReader;
import com.tyb.myblog.v2.comment.domain.CommentType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
/**
 * 基于旧库评论表的后台评论读取器。
 *
 * <p>后台可以按审核状态、删除状态、类型、主题和关键词查询评论，并读取审核、删除、
 * 恢复等审计字段。</p>
 */
public class DatabaseAdminCommentReader implements AdminCommentReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseAdminCommentReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询后台评论分页列表。
     */
    @Override
    public PageResponse<AdminCommentItem> list(AdminCommentQuery query) {
        SqlParts parts = buildWhere(query);
        Long total = jdbcTemplate.queryForObject("""
                        select count(*)
                        from t_comment c
                        join t_user_info u on u.id = c.user_id
                        left join t_article a on a.id = c.topic_id
                        %s
                        """.formatted(parts.where()),
                Long.class,
                parts.args().toArray());
        List<Object> args = new ArrayList<>(parts.args());
        args.add(query.size());
        args.add(query.offset());
        List<AdminCommentItem> records = jdbcTemplate.query("""
                        select c.id,
                               c.type,
                               c.topic_id,
                               a.article_title as topic_title,
                               c.parent_id,
                               c.reply_user_id,
                               c.user_id,
                               u.nickname,
                               u.avatar,
                               c.comment_content,
                               c.is_review,
                               c.is_delete,
                               c.create_time,
                               c.update_time
                        from t_comment c
                        join t_user_info u on u.id = c.user_id
                        left join t_article a on a.id = c.topic_id
                        %s
                        order by c.id desc
                        limit ? offset ?
                        """.formatted(parts.where()),
                (rs, rowNum) -> new AdminCommentItem(
                        rs.getInt("id"),
                        CommentType.fromCode(rs.getInt("type")),
                        (Integer) rs.getObject("topic_id"),
                        rs.getString("topic_title"),
                        (Integer) rs.getObject("parent_id"),
                        (Integer) rs.getObject("reply_user_id"),
                        rs.getInt("user_id"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("comment_content"),
                        rs.getInt("is_review") == 1,
                        rs.getInt("is_delete") == 1,
                        toLocalDateTime(rs.getTimestamp("create_time")),
                        toLocalDateTime(rs.getTimestamp("update_time"))),
                args.toArray());
        return new PageResponse<>(records, total == null ? 0 : total, query.page(), query.size());
    }

    /**
     * 查询后台评论详情。
     */
    @Override
    public Optional<AdminCommentDetail> findDetail(int id) {
        List<AdminCommentDetail> records = jdbcTemplate.query("""
                        select c.id,
                               c.type,
                               c.topic_id,
                               a.article_title as topic_title,
                               c.parent_id,
                               c.reply_user_id,
                               reply_user.nickname as reply_nickname,
                               c.user_id,
                               u.nickname,
                               u.avatar,
                               c.comment_content,
                               c.is_review,
                               c.is_delete,
                               c.create_ip,
                               c.user_agent,
                               c.reviewed_by,
                               c.review_time,
                               c.deleted_by,
                               c.delete_time,
                               c.restored_by,
                               c.restore_time,
                               c.create_time,
                               c.update_time
                        from t_comment c
                        join t_user_info u on u.id = c.user_id
                        left join t_user_info reply_user on reply_user.id = c.reply_user_id
                        left join t_article a on a.id = c.topic_id
                        where c.id = ?
                        """,
                (rs, rowNum) -> new AdminCommentDetail(
                        rs.getInt("id"),
                        CommentType.fromCode(rs.getInt("type")),
                        (Integer) rs.getObject("topic_id"),
                        rs.getString("topic_title"),
                        (Integer) rs.getObject("parent_id"),
                        (Integer) rs.getObject("reply_user_id"),
                        rs.getString("reply_nickname"),
                        rs.getInt("user_id"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("comment_content"),
                        rs.getInt("is_review") == 1,
                        rs.getInt("is_delete") == 1,
                        rs.getString("create_ip"),
                        rs.getString("user_agent"),
                        (Integer) rs.getObject("reviewed_by"),
                        toLocalDateTime(rs.getTimestamp("review_time")),
                        (Integer) rs.getObject("deleted_by"),
                        toLocalDateTime(rs.getTimestamp("delete_time")),
                        (Integer) rs.getObject("restored_by"),
                        toLocalDateTime(rs.getTimestamp("restore_time")),
                        toLocalDateTime(rs.getTimestamp("create_time")),
                        toLocalDateTime(rs.getTimestamp("update_time"))),
                id);
        return records.stream().findFirst();
    }

    /**
     * 根据后台筛选条件构造 SQL where 片段和参数。
     */
    private SqlParts buildWhere(AdminCommentQuery query) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        clauses.add("c.is_delete = ?");
        args.add(query.deleted() ? 1 : 0);
        if (query.type() != null) {
            clauses.add("c.type = ?");
            args.add(query.type().code());
        }
        if (query.topicId() != null) {
            clauses.add("c.topic_id = ?");
            args.add(query.topicId());
        }
        if (query.reviewed() != null) {
            clauses.add("c.is_review = ?");
            args.add(query.reviewed() ? 1 : 0);
        }
        if (query.keyword() != null) {
            clauses.add("(c.comment_content like ? or u.nickname like ?)");
            String keyword = "%" + query.keyword() + "%";
            args.add(keyword);
            args.add(keyword);
        }
        return new SqlParts("where " + String.join(" and ", clauses), args);
    }

    /**
     * 读取可为空的时间字段。
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * SQL 条件片段和参数。
     */
    private record SqlParts(String where, List<Object> args) {
    }
}
