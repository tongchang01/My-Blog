package com.tyb.myblog.v2.comment.infrastructure;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.comment.domain.AdminCommentDeletionCommand;
import com.tyb.myblog.v2.comment.domain.AdminCommentModerationCommand;
import com.tyb.myblog.v2.comment.domain.AdminCommentModerator;
import com.tyb.myblog.v2.comment.domain.AdminCommentRestoreCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DatabaseAdminCommentModerator implements AdminCommentModerator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseAdminCommentModerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int review(AdminCommentModerationCommand command) {
        List<Integer> ids = normalizeIds(command.ids());
        return review(ids, command.reviewed() ? 1 : 0, command.operatorUserId());
    }

    @Override
    public int delete(AdminCommentDeletionCommand command) {
        List<Integer> ids = normalizeIds(command.ids());
        return delete(ids, command.operatorUserId());
    }

    @Override
    public int restore(AdminCommentRestoreCommand command) {
        List<Integer> ids = normalizeIds(command.ids());
        return restore(ids, command.operatorUserId());
    }

    private int review(List<Integer> ids, int reviewed, int operatorUserId) {
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Object[] args = new Object[ids.size() + 4];
        args[0] = reviewed;
        args[1] = operatorUserId;
        args[2] = now;
        args[3] = now;
        for (int i = 0; i < ids.size(); i++) {
            args[i + 4] = ids.get(i);
        }
        return jdbcTemplate.update("""
                update t_comment
                set is_review = ?,
                    reviewed_by = ?,
                    review_time = ?,
                    update_time = ?
                where id in (%s)
                """.formatted(placeholders), args);
    }

    private int delete(List<Integer> ids, int operatorUserId) {
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Object[] args = new Object[ids.size() + 3];
        args[0] = operatorUserId;
        args[1] = now;
        args[2] = now;
        for (int i = 0; i < ids.size(); i++) {
            args[i + 3] = ids.get(i);
        }
        return jdbcTemplate.update("""
                update t_comment
                set is_delete = 1,
                    deleted_by = ?,
                    delete_time = ?,
                    update_time = ?
                where id in (%s)
                """.formatted(placeholders), args);
    }

    private int restore(List<Integer> ids, int operatorUserId) {
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Object[] args = new Object[ids.size() + 3];
        args[0] = operatorUserId;
        args[1] = now;
        args[2] = now;
        for (int i = 0; i < ids.size(); i++) {
            args[i + 3] = ids.get(i);
        }
        return jdbcTemplate.update("""
                update t_comment
                set is_delete = 0,
                    restored_by = ?,
                    restore_time = ?,
                    update_time = ?
                where id in (%s)
                """.formatted(placeholders), args);
    }

    private List<Integer> normalizeIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "评论 ID 不能为空");
        }
        List<Integer> normalized = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "评论 ID 不能为空");
        }
        if (normalized.size() > 100) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "单次最多处理 100 条评论");
        }
        return normalized;
    }
}
