package com.aurora.myblog.v2.modules.comment.infrastructure;

import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentItem;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentQuery;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentReader;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseAdminCommentReader implements AdminCommentReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseAdminCommentReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    private SqlParts buildWhere(AdminCommentQuery query) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        clauses.add("c.is_delete = 0");
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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record SqlParts(String where, List<Object> args) {
    }
}
