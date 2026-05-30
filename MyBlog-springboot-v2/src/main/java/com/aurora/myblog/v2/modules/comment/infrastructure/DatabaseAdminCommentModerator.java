package com.aurora.myblog.v2.modules.comment.infrastructure;

import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDeletionCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerationCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerator;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentRestoreCommand;
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
        return update(ids, "is_review", command.reviewed() ? 1 : 0);
    }

    @Override
    public int delete(AdminCommentDeletionCommand command) {
        List<Integer> ids = normalizeIds(command.ids());
        return update(ids, "is_delete", 1);
    }

    @Override
    public int restore(AdminCommentRestoreCommand command) {
        List<Integer> ids = normalizeIds(command.ids());
        return update(ids, "is_delete", 0);
    }

    private int update(List<Integer> ids, String column, int value) {
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        Object[] args = new Object[ids.size() + 2];
        args[0] = value;
        args[1] = Timestamp.valueOf(LocalDateTime.now());
        for (int i = 0; i < ids.size(); i++) {
            args[i + 2] = ids.get(i);
        }
        return jdbcTemplate.update("""
                update t_comment
                set %s = ?, update_time = ?
                where id in (%s)
                """.formatted(column, placeholders), args);
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
