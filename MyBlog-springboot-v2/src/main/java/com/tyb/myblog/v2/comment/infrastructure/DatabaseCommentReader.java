package com.tyb.myblog.v2.comment.infrastructure;

import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.comment.domain.CommentAuthor;
import com.tyb.myblog.v2.comment.domain.CommentPageQuery;
import com.tyb.myblog.v2.comment.domain.CommentReader;
import com.tyb.myblog.v2.comment.domain.CommentReply;
import com.tyb.myblog.v2.comment.domain.CommentThread;
import com.tyb.myblog.v2.comment.domain.CommentType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DatabaseCommentReader implements CommentReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCommentReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PageResponse<CommentThread> listComments(CommentType type, Integer topicId, CommentPageQuery query) {
        Long total = jdbcTemplate.queryForObject("""
                select count(*)
                from t_comment c
                where c.is_delete = 0
                  and c.is_review = 1
                  and c.parent_id is null
                  and c.type = ?
                  and ((? is null and c.topic_id is null) or c.topic_id = ?)
                """, Long.class, type.code(), topicId, topicId);
        List<CommentThread> roots = loadRootComments(type, topicId, query);
        return new PageResponse<>(attachReplies(roots), total == null ? 0 : total, query.page(), query.size());
    }

    @Override
    public List<CommentReply> listRepliesByCommentId(int commentId) {
        return loadReplies(List.of(commentId));
    }

    @Override
    public List<CommentThread> listTopComments(int limit) {
        List<CommentThread> roots = jdbcTemplate.query("""
                        select c.id,
                               c.type,
                               c.topic_id,
                               c.comment_content,
                               c.create_time,
                               u.id as user_id,
                               u.nickname,
                               u.avatar,
                               u.website
                        from t_comment c
                        join t_user_info u on u.id = c.user_id
                        where c.is_delete = 0
                          and c.is_review = 1
                          and c.parent_id is null
                        order by c.id desc
                        limit ?
                        """,
                (rs, rowNum) -> toThread(new RootRow(
                        rs.getInt("id"),
                        CommentType.fromCode(rs.getInt("type")),
                        (Integer) rs.getObject("topic_id"),
                        rs.getString("comment_content"),
                        toLocalDateTime(rs.getTimestamp("create_time")),
                        rs.getInt("user_id"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("website"))),
                Math.max(1, Math.min(limit, 20)));
        return attachReplies(roots);
    }

    private List<CommentThread> loadRootComments(CommentType type, Integer topicId, CommentPageQuery query) {
        return jdbcTemplate.query("""
                        select c.id,
                               c.type,
                               c.topic_id,
                               c.comment_content,
                               c.create_time,
                               u.id as user_id,
                               u.nickname,
                               u.avatar,
                               u.website
                        from t_comment c
                        join t_user_info u on u.id = c.user_id
                        where c.is_delete = 0
                          and c.is_review = 1
                          and c.parent_id is null
                          and c.type = ?
                          and ((? is null and c.topic_id is null) or c.topic_id = ?)
                        order by c.id desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> toThread(new RootRow(
                        rs.getInt("id"),
                        CommentType.fromCode(rs.getInt("type")),
                        (Integer) rs.getObject("topic_id"),
                        rs.getString("comment_content"),
                        toLocalDateTime(rs.getTimestamp("create_time")),
                        rs.getInt("user_id"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("website"))),
                type.code(), topicId, topicId, query.size(), query.offset());
    }

    private List<CommentThread> attachReplies(List<CommentThread> roots) {
        if (roots.isEmpty()) {
            return List.of();
        }
        List<Integer> rootIds = roots.stream().map(CommentThread::id).toList();
        Map<Integer, List<CommentReply>> repliesByRootId = loadReplies(rootIds).stream()
                .collect(Collectors.groupingBy(CommentReply::parentId, LinkedHashMap::new, Collectors.toList()));
        return roots.stream()
                .map(root -> new CommentThread(
                        root.id(),
                        root.type(),
                        root.topicId(),
                        root.author(),
                        root.content(),
                        root.createdAt(),
                        repliesByRootId.getOrDefault(root.id(), List.of())))
                .toList();
    }

    private List<CommentReply> loadReplies(List<Integer> parentIds) {
        if (parentIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", parentIds.stream().map(id -> "?").toList());
        return jdbcTemplate.query("""
                        select c.id,
                               c.parent_id,
                               c.comment_content,
                               c.create_time,
                               u.id as user_id,
                               u.nickname,
                               u.avatar,
                               u.website,
                               r.id as reply_user_id,
                               r.nickname as reply_nickname,
                               r.avatar as reply_avatar,
                               r.website as reply_website
                        from t_comment c
                        join t_user_info u on u.id = c.user_id
                        join t_user_info r on r.id = c.reply_user_id
                        where c.is_delete = 0
                          and c.is_review = 1
                          and c.parent_id in (%s)
                        order by c.create_time asc, c.id asc
                        """.formatted(placeholders),
                (rs, rowNum) -> new CommentReply(
                        rs.getInt("id"),
                        rs.getInt("parent_id"),
                        new CommentAuthor(
                                rs.getInt("user_id"),
                                rs.getString("nickname"),
                                rs.getString("avatar"),
                                rs.getString("website")),
                        new CommentAuthor(
                                rs.getInt("reply_user_id"),
                                rs.getString("reply_nickname"),
                                rs.getString("reply_avatar"),
                                rs.getString("reply_website")),
                        rs.getString("comment_content"),
                        toLocalDateTime(rs.getTimestamp("create_time"))),
                parentIds.toArray());
    }

    private CommentThread toThread(RootRow row) {
        return new CommentThread(
                row.id(),
                row.type(),
                row.topicId(),
                new CommentAuthor(row.userId(), row.nickname(), row.avatar(), row.website()),
                row.content(),
                row.createdAt(),
                List.of());
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record RootRow(
            int id,
            CommentType type,
            Integer topicId,
            String content,
            LocalDateTime createdAt,
            int userId,
            String nickname,
            String avatar,
            String website
    ) {
    }
}
