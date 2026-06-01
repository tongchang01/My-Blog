package com.tyb.myblog.v2.comment.infrastructure;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.comment.domain.CommentCreateCommand;
import com.tyb.myblog.v2.comment.domain.CommentType;
import com.tyb.myblog.v2.comment.domain.CommentWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 基于旧库评论表的前台评论写入器。
 *
 * <p>写入 {@code t_comment} 时会记录评论人、主题、父评论、回复用户、客户端 IP 和 User-Agent。
 * 当前新评论默认写入未审核状态，前台展示由读取端过滤审核状态。</p>
 */
@Component
public class DatabaseCommentWriter implements CommentWriter {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCommentWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 校验并保存评论。
     */
    @Override
    public int save(CommentCreateCommand command) {
        validate(command);
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into t_comment (
                        user_id, reply_user_id, topic_id, comment_content,
                        parent_id, type, is_delete, is_review,
                        create_ip, user_agent,
                        create_time, update_time
                    )
                    -- is_delete = 0 表示未删除，is_review = 0 表示待审核。
                    values (?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, command.userId());
            setNullableInt(ps, 2, command.replyUserId());
            setNullableInt(ps, 3, command.topicId());
            ps.setString(4, command.content().trim());
            setNullableInt(ps, 5, command.parentId());
            ps.setInt(6, command.type().code());
            ps.setString(7, command.clientIp());
            ps.setString(8, command.userAgent());
            ps.setTimestamp(9, Timestamp.valueOf(now));
            ps.setTimestamp(10, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR, "评论保存失败");
        }
        return key.intValue();
    }

    /**
     * 校验评论提交业务边界。
     */
    private void validate(CommentCreateCommand command) {
        if (command.type() == CommentType.TALK) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "说说评论暂未支持");
        }
        if (command.content() == null || command.content().isBlank()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "评论内容不能为空");
        }
        if (command.type() == CommentType.ARTICLE) {
            validateArticleTarget(command.topicId());
        }
        if (command.type().forbidsTopic() && command.topicId() != null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "该评论类型不能指定主题");
        }
        if (command.parentId() == null && command.replyUserId() != null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "根评论不能指定回复用户");
        }
        if (command.parentId() != null) {
            validateReply(command);
        }
    }

    /**
     * 校验文章评论的目标文章是否存在且可评论。
     */
    private void validateArticleTarget(Integer topicId) {
        if (topicId == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "文章评论必须指定文章");
        }
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from t_article
                where id = ?
                  and is_delete = 0
                  -- status in (1, 2) 表示公开文章或受保护文章可以接收评论。
                  and status in (1, 2)
                """, Integer.class, topicId);
        if (count == null || count == 0) {
            throw new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在");
        }
    }

    /**
     * 校验回复关系。
     */
    private void validateReply(CommentCreateCommand command) {
        if (command.replyUserId() == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "回复评论必须指定回复用户");
        }
        List<ParentComment> parents = jdbcTemplate.query("""
                        select id, parent_id, topic_id, type
                        from t_comment
                        where id = ?
                          and is_delete = 0
                        """,
                (rs, rowNum) -> new ParentComment(
                        rs.getInt("id"),
                        (Integer) rs.getObject("parent_id"),
                        (Integer) rs.getObject("topic_id"),
                        rs.getInt("type")),
                command.parentId());
        ParentComment parent = parents.stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "父评论不存在"));
        if (parent.parentId() != null) {
            // 旧库当前只允许二级评论，不能继续回复回复。
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "只能回复根评论");
        }
        if (parent.type() != command.type().code()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "回复类型必须和父评论一致");
        }
        if (!Objects.equals(parent.topicId(), command.topicId())) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "回复主题必须和父评论一致");
        }
        Integer userCount = jdbcTemplate.queryForObject("""
                select count(*)
                from t_user_info
                where id = ?
                  and is_disable = 0
                """, Integer.class, command.replyUserId());
        if (userCount == null || userCount == 0) {
            throw new ApiException(ApiErrorCode.NOT_FOUND, "回复用户不存在");
        }
    }

    /**
     * 写入可为空的整数值。
     */
    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws java.sql.SQLException {
        if (value == null) {
            ps.setObject(index, null);
        } else {
            ps.setInt(index, value);
        }
    }

    /**
     * 父评论校验所需的旧库字段。
     */
    private record ParentComment(int id, Integer parentId, Integer topicId, int type) {
    }
}
